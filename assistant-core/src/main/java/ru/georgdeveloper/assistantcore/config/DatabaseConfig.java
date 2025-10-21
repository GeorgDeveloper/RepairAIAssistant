package ru.georgdeveloper.assistantcore.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import com.zaxxer.hikari.HikariDataSource;

/**
 * Конфигурация базы данных и JPA репозиториев
 */
@Configuration
@EnableTransactionManagement
public class DatabaseConfig {

    @Value("${data-sync.sql_server.url}")
    private String sqlServerUrl;
    
    @Value("${data-sync.sql_server.username}")
    private String sqlServerUsername;
    
    @Value("${data-sync.sql_server.password}")
    private String sqlServerPassword;
    
    @Value("${data-sync.sql_server.driver}")
    private String sqlServerDriver;

    /**
     * Источник данных для SQL Server
     */
    @Bean(name = "sqlServerDataSource")
    public DataSource sqlServerDataSource() {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(sqlServerUrl);
        ds.setUsername(sqlServerUsername);
        ds.setPassword(sqlServerPassword);
        ds.setDriverClassName(sqlServerDriver);
        ds.setMinimumIdle(1);
        ds.setMaximumPoolSize(5);
        ds.setIdleTimeout(60000);
        ds.setConnectionTimeout(30000);
        ds.setPoolName("sqlserver-hikari");
        return ds;
    }

    /**
     * JdbcTemplate для работы с SQL Server
     */
    @Bean(name = "sqlServerJdbcTemplate")
    public JdbcTemplate sqlServerJdbcTemplate() {
        return new JdbcTemplate(sqlServerDataSource());
    }
}