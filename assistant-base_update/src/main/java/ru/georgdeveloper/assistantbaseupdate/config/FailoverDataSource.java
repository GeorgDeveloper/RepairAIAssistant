package ru.georgdeveloper.assistantbaseupdate.config;

import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DataSource с поддержкой failover для подключения к SQL Server.
 * 
 * Пробует подключиться к основному URL, а при неудаче - к альтернативным URL из списка.
 * После успешного подключения использует рабочий URL для всех последующих подключений.
 */
public class FailoverDataSource implements DataSource {

    private static final Logger logger = LoggerFactory.getLogger(FailoverDataSource.class);

    private final List<String> urls;
    private final String username;
    private final String password;
    private final String driver;
    private final int connectionTimeout;
    private final String poolName;

    private volatile HikariDataSource activeDataSource;
    private volatile int activeUrlIndex = -1;

    public FailoverDataSource(List<String> urls, String username, String password, 
                             String driver, int connectionTimeout, String poolName) {
        if (urls == null || urls.isEmpty()) {
            throw new IllegalArgumentException("URLs list cannot be null or empty");
        }
        this.urls = new ArrayList<>(urls);
        this.username = username;
        this.password = password;
        this.driver = driver;
        this.connectionTimeout = connectionTimeout;
        this.poolName = poolName;
    }

    /**
     * Получает активный DataSource или создает новый, пробуя подключиться к доступному URL
     */
    private synchronized HikariDataSource getActiveDataSource() throws SQLException {
        // Если уже есть рабочий DataSource, проверяем его
        if (activeDataSource != null && activeUrlIndex >= 0) {
            try {
                // Проверяем, что соединение еще работает
                try (Connection conn = activeDataSource.getConnection()) {
                    if (conn.isValid(2)) { // Проверка за 2 секунды
                        return activeDataSource;
                    }
                }
            } catch (SQLException e) {
                logger.warn("Активное подключение к URL #{} больше не работает, пробуем другие: {}", 
                           activeUrlIndex + 1, e.getMessage());
                // Закрываем старое подключение
                closeActiveDataSource();
            }
        }

        // Пробуем подключиться к каждому URL по очереди
        SQLException lastException = null;
        HikariDataSource tempDs = null;
        
        for (int i = 0; i < urls.size(); i++) {
            String url = urls.get(i);
            try {
                logger.info("Попытка подключения к SQL Server (URL #{}): {}", i + 1, maskUrl(url));
                
                tempDs = createDataSource(url);
                
                // Проверяем подключение
                try (Connection conn = tempDs.getConnection()) {
                    if (conn.isValid(2)) {
                        logger.info("Успешное подключение к SQL Server (URL #{}): {}", i + 1, maskUrl(url));
                        // Закрываем предыдущий DataSource, если был
                        if (activeDataSource != null && activeDataSource != tempDs) {
                            try {
                                activeDataSource.close();
                            } catch (Exception e) {
                                logger.debug("Ошибка при закрытии предыдущего DataSource: {}", e.getMessage());
                            }
                        }
                        activeDataSource = tempDs;
                        activeUrlIndex = i;
                        return tempDs;
                    }
                }
            } catch (SQLException e) {
                lastException = e;
                logger.warn("Не удалось подключиться к URL #{} ({}): {}", 
                           i + 1, maskUrl(url), e.getMessage());
                // Закрываем временный DataSource
                if (tempDs != null) {
                    try {
                        tempDs.close();
                    } catch (Exception closeEx) {
                        logger.debug("Ошибка при закрытии временного DataSource: {}", closeEx.getMessage());
                    }
                    tempDs = null;
                }
                // Продолжаем пробовать следующий URL
            }
        }

        // Если ни один URL не сработал
        String errorMsg = "Не удалось подключиться ни к одному из " + urls.size() + " URL для SQL Server";
        logger.error(errorMsg);
        if (lastException != null) {
            throw new SQLException(errorMsg, lastException);
        }
        throw new SQLException(errorMsg);
    }

    /**
     * Создает HikariDataSource для указанного URL
     */
    private HikariDataSource createDataSource(String url) {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(url);
        ds.setUsername(username);
        ds.setPassword(password);
        ds.setDriverClassName(driver);
        ds.setMinimumIdle(1);
        ds.setMaximumPoolSize(5);
        ds.setIdleTimeout(60000);
        ds.setConnectionTimeout(connectionTimeout);
        ds.setPoolName(poolName + "-" + (activeUrlIndex >= 0 ? activeUrlIndex : "temp"));
        return ds;
    }

    /**
     * Закрывает активный DataSource
     */
    private void closeActiveDataSource() {
        if (activeDataSource != null) {
            try {
                activeDataSource.close();
            } catch (Exception e) {
                logger.warn("Ошибка при закрытии DataSource: {}", e.getMessage());
            }
            activeDataSource = null;
            activeUrlIndex = -1;
        }
    }

    /**
     * Маскирует URL для безопасного логирования (скрывает пароль, если есть)
     */
    private String maskUrl(String url) {
        if (url == null) {
            return "null";
        }
        // Убираем чувствительную информацию из URL
        return url.replaceAll("password=[^;]+", "password=***");
    }

    @Override
    public Connection getConnection() throws SQLException {
        return getActiveDataSource().getConnection();
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return getActiveDataSource().getConnection(username, password);
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isInstance(this)) {
            return iface.cast(this);
        }
        return getActiveDataSource().unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return iface.isInstance(this) || getActiveDataSource().isWrapperFor(iface);
    }

    @Override
    public java.io.PrintWriter getLogWriter() throws SQLException {
        return getActiveDataSource().getLogWriter();
    }

    @Override
    public void setLogWriter(java.io.PrintWriter out) throws SQLException {
        getActiveDataSource().setLogWriter(out);
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        getActiveDataSource().setLoginTimeout(seconds);
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return getActiveDataSource().getLoginTimeout();
    }

    @Override
    public java.util.logging.Logger getParentLogger() throws java.sql.SQLFeatureNotSupportedException {
        try {
            return getActiveDataSource().getParentLogger();
        } catch (SQLException e) {
            throw new java.sql.SQLFeatureNotSupportedException("Unable to get parent logger: " + e.getMessage(), e);
        }
    }

    /**
     * Закрывает все подключения и освобождает ресурсы
     */
    public void close() {
        closeActiveDataSource();
    }
}
