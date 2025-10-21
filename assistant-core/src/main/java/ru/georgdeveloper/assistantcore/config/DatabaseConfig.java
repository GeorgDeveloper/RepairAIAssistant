package ru.georgdeveloper.assistantcore.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
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
@EnableJpaRepositories(basePackages = "ru.georgdeveloper.assistantcore.repository")
public class DatabaseConfig {

    @Value("${data-sync.sql_server.url}")
    private String sqlServerUrl;
    
    @Value("${data-sync.sql_server.username}")
    private String sqlServerUsername;
    
    @Value("${data-sync.sql_server.password}")
    private String sqlServerPassword;
    
    @Value("${data-sync.sql_server.driver}")
    private String sqlServerDriver;

    @Value("${spring.datasource.url}")
    private String mysqlUrl;
    
    @Value("${spring.datasource.username}")
    private String mysqlUsername;
    
    @Value("${spring.datasource.password}")
    private String mysqlPassword;
    
    @Value("${spring.datasource.driver-class-name}")
    private String mysqlDriver;

    /**
     * Основной источник данных для MySQL (для JPA сущностей)
     */
    @Bean(name = "dataSource")
    @Primary
    public DataSource dataSource() {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(mysqlUrl);
        ds.setUsername(mysqlUsername);
        ds.setPassword(mysqlPassword);
        ds.setDriverClassName(mysqlDriver);
        ds.setMinimumIdle(1);
        ds.setMaximumPoolSize(10);
        ds.setIdleTimeout(60000);
        ds.setConnectionTimeout(30000);
        ds.setPoolName("mysql-hikari");
        return ds;
    }

    /**
     * Источник данных для SQL Server (для прямых запросов)
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
     * JdbcTemplate для работы с MySQL
     */
    @Bean(name = "jdbcTemplate")
    @Primary
    public JdbcTemplate jdbcTemplate() {
        return new JdbcTemplate(dataSource());
    }

    /**
     * JdbcTemplate для работы с SQL Server
     */
    @Bean(name = "sqlServerJdbcTemplate")
    public JdbcTemplate sqlServerJdbcTemplate() {
        return new JdbcTemplate(sqlServerDataSource());
    }
}