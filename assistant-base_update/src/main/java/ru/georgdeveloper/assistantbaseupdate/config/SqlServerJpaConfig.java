package ru.georgdeveloper.assistantbaseupdate.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
    basePackages = "ru.georgdeveloper.assistantbaseupdate.repository.sqlserver",
    entityManagerFactoryRef = "sqlServerEntityManagerFactory",
    transactionManagerRef = "sqlServerTransactionManager"
)
public class SqlServerJpaConfig {

    @Bean
    public LocalContainerEntityManagerFactoryBean sqlServerEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("sqlServerDataSource") DataSource dataSource) {
        
        Map<String, Object> properties = new HashMap<>();
        properties.put("hibernate.hbm2ddl.auto", "none");
        properties.put("hibernate.dialect", "org.hibernate.dialect.SQLServerDialect");
        properties.put("hibernate.show_sql", "false");
        properties.put("hibernate.format_sql", "true");
        properties.put("hibernate.physical_naming_strategy", "org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl");
        properties.put("hibernate.implicit_naming_strategy", "org.hibernate.boot.model.naming.ImplicitNamingStrategyLegacyJpaImpl");
        
        return builder
                .dataSource(dataSource)
                .packages("ru.georgdeveloper.assistantbaseupdate.entity.sqlserver")
                .persistenceUnit("sqlserver")
                .properties(properties)
                .build();
    }

    @Bean
    public PlatformTransactionManager sqlServerTransactionManager(
            @Qualifier("sqlServerEntityManagerFactory") LocalContainerEntityManagerFactoryBean entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory.getObject() != null ? entityManagerFactory.getObject() : null);
    }
}
