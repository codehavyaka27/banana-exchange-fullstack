package com.bananatrading.engine.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

@Configuration
public class GlobalCorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // --- THIS IS THE MAGIC LINE THAT FIXES YOUR ERROR ---
        config.setAllowCredentials(true);

        // Explicitly whitelist your Vite React app
        config.setAllowedOriginPatterns(List.of("http://localhost:5173"));

        // Allow all standard headers and WebSocket upgrade headers
        config.setAllowedHeaders(Arrays.asList("Origin", "Content-Type", "Accept", "Authorization", "Cache-Control"));

        // Allow all HTTP methods
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "OPTIONS", "DELETE", "PATCH"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        // Apply this VIP rule to EVERYTHING, including the /ws-market/** endpoints
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}