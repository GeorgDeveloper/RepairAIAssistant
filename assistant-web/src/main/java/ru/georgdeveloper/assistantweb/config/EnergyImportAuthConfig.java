package ru.georgdeveloper.assistantweb.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.georgdeveloper.assistantweb.filter.EnergyImportAuthFilter;

@Configuration
public class EnergyImportAuthConfig {

    @Bean
    public FilterRegistrationBean<EnergyImportAuthFilter> energyImportAuthFilter() {
        FilterRegistrationBean<EnergyImportAuthFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new EnergyImportAuthFilter());
        registration.addUrlPatterns("/energy/import", "/energy/import/*", "/api/energy/import");
        registration.setOrder(1);
        return registration;
    }
}
