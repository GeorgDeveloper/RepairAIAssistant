package ru.georgdeveloper.assistantbaseupdate.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import com.zaxxer.hikari.HikariDataSource;
import java.util.ArrayList;
import java.util.List;

/**
 * Конфигурация источников данных для синхронизации.
 * 
 * Настраивает подключения к SQL Server (источник) и MySQL (назначение)
 * для синхронизации данных между базами.
 * 
 * SQL Server поддерживает failover - пробует подключиться к основному URL,
 * а при неудаче - к альтернативным URL из списка fallback_urls.
 */
@Configuration
public class DatabaseConfig {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);

    @Autowired
    private DataSyncProperties dataSyncProperties;

    /**
     * Источник данных для SQL Server с поддержкой failover
     * Пробует подключиться к основному URL, а при неудаче - к альтернативным
     */
    @Bean(name = "sqlServerDataSource")
    public DataSource sqlServerDataSource() {
        DataSyncProperties.SqlServer sqlServerConfig = dataSyncProperties.getSqlServer();
        
        // Формируем список URL для подключения: основной + альтернативные
        List<String> urls = new ArrayList<>();
        urls.add(sqlServerConfig.getUrl()); // Основной URL
        
        if (sqlServerConfig.getFallbackUrls() != null && !sqlServerConfig.getFallbackUrls().isEmpty()) {
            urls.addAll(sqlServerConfig.getFallbackUrls());
            logger.info("Настроен failover для SQL Server: основной URL + {} альтернативных", 
                       sqlServerConfig.getFallbackUrls().size());
        } else {
            logger.info("Используется только основной URL для SQL Server (без failover)");
        }
        
        return new FailoverDataSource(
            urls,
            sqlServerConfig.getUsername(),
            sqlServerConfig.getPassword(),
            sqlServerConfig.getDriver(),
            sqlServerConfig.getTimeout() * 1000, // Конвертируем секунды в миллисекунды
            "sqlserver-hikari"
        );
    }

    /**
     * Источник данных для MySQL (назначение данных)
     */
    @Bean(name = "mysqlDataSource")
    @Primary
    public DataSource mysqlDataSource() {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(dataSyncProperties.getMysql().getUrl());
        ds.setUsername(dataSyncProperties.getMysql().getUsername());
        ds.setPassword(dataSyncProperties.getMysql().getPassword());
        ds.setDriverClassName(dataSyncProperties.getMysql().getDriver());
        ds.setMinimumIdle(1);
        ds.setMaximumPoolSize(5);
        ds.setIdleTimeout(60000);
        ds.setConnectionTimeout(30000);
        ds.setPoolName("mysql-hikari");
        return ds;
    }

    /**
     * JdbcTemplate для работы с SQL Server
     */
    @Bean(name = "sqlServerJdbcTemplate")
    public JdbcTemplate sqlServerJdbcTemplate() {
        return new JdbcTemplate(sqlServerDataSource());
    }

    /**
     * JdbcTemplate для работы с MySQL
     */
    @Bean(name = "mysqlJdbcTemplate")
    public JdbcTemplate mysqlJdbcTemplate() {
        return new JdbcTemplate(mysqlDataSource());
    }
}
