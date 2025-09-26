package ru.georgdeveloper.assistantbaseupdate.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import com.zaxxer.hikari.HikariDataSource;

/**
 * Конфигурация источников данных для синхронизации.
 * 
 * Настраивает подключения к SQL Server (источник) и MySQL (назначение)
 * для синхронизации данных между базами.
 */
@Configuration
public class DatabaseConfig {

    @Autowired
    private DataSyncProperties dataSyncProperties;

    /**
     * Источник данных для SQL Server (источник данных)
     */
    @Bean(name = "sqlServerDataSource")
    public DataSource sqlServerDataSource() {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(dataSyncProperties.getSqlServer().getUrl());
        ds.setUsername(dataSyncProperties.getSqlServer().getUsername());
        ds.setPassword(dataSyncProperties.getSqlServer().getPassword());
        ds.setDriverClassName(dataSyncProperties.getSqlServer().getDriver());
        ds.setMinimumIdle(1);
        ds.setMaximumPoolSize(5);
        ds.setIdleTimeout(60000);
        ds.setConnectionTimeout(30000);
        ds.setPoolName("sqlserver-hikari");
        return ds;
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
