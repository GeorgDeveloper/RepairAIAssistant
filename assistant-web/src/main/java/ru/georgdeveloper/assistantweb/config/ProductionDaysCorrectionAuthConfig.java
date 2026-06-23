package ru.georgdeveloper.assistantweb.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.georgdeveloper.assistantweb.filter.ProductionDaysCorrectionAuthFilter;

@Configuration
public class ProductionDaysCorrectionAuthConfig {

    @Bean
    public FilterRegistrationBean<ProductionDaysCorrectionAuthFilter> productionDaysCorrectionAuthFilter() {
        FilterRegistrationBean<ProductionDaysCorrectionAuthFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new ProductionDaysCorrectionAuthFilter());
        registration.addUrlPatterns("/production-days-correction", "/production-days-correction/*", "/api/production-days-correction", "/api/production-days-correction/*");
        registration.setOrder(1);
        return registration;
    }
}
