package ru.georgdeveloper.assistantbaseupdate.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

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
    @Primary
    public DataSource sqlServerDataSource() {
        org.apache.tomcat.jdbc.pool.DataSource dataSource = new org.apache.tomcat.jdbc.pool.DataSource();
        dataSource.setUrl(dataSyncProperties.getSqlServer().getUrl());
        dataSource.setUsername(dataSyncProperties.getSqlServer().getUsername());
        dataSource.setPassword(dataSyncProperties.getSqlServer().getPassword());
        dataSource.setDriverClassName(dataSyncProperties.getSqlServer().getDriver());
        dataSource.setInitialSize(1);
        dataSource.setMaxActive(5);
        dataSource.setMaxIdle(5);
        dataSource.setMinIdle(1);
        dataSource.setValidationQuery("SELECT 1");
        dataSource.setTestOnBorrow(true);
        dataSource.setTestWhileIdle(true);
        dataSource.setTimeBetweenEvictionRunsMillis(30000);
        dataSource.setMinEvictableIdleTimeMillis(60000);
        return dataSource;
    }

    /**
     * Источник данных для MySQL (назначение данных)
     */
    @Bean(name = "mysqlDataSource")
    public DataSource mysqlDataSource() {
        org.apache.tomcat.jdbc.pool.DataSource dataSource = new org.apache.tomcat.jdbc.pool.DataSource();
        dataSource.setUrl(dataSyncProperties.getMysql().getUrl());
        dataSource.setUsername(dataSyncProperties.getMysql().getUsername());
        dataSource.setPassword(dataSyncProperties.getMysql().getPassword());
        dataSource.setDriverClassName(dataSyncProperties.getMysql().getDriver());
        dataSource.setInitialSize(1);
        dataSource.setMaxActive(5);
        dataSource.setMaxIdle(5);
        dataSource.setMinIdle(1);
        dataSource.setValidationQuery("SELECT 1");
        dataSource.setTestOnBorrow(true);
        dataSource.setTestWhileIdle(true);
        dataSource.setTimeBetweenEvictionRunsMillis(30000);
        dataSource.setMinEvictableIdleTimeMillis(60000);
        return dataSource;
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
