package ru.georgdeveloper.assistantcore.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.Nullable;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

/**
 * Конфигурация базы данных и JPA репозиториев
 */
@Configuration
@EnableJpaRepositories(basePackages = "ru.georgdeveloper.assistantcore.repository")
@EnableTransactionManagement
public class DatabaseConfig {

    @Bean
    @Primary
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource) {
            @Override
            public List<Map<String, Object>> queryForList(String sql) {
                return query(sql, new SafeColumnMapRowMapper());
            }

            @Override
            public List<Map<String, Object>> queryForList(String sql, @Nullable Object... args) {
                return query(sql, new SafeColumnMapRowMapper(), args);
            }
        };
    }
}