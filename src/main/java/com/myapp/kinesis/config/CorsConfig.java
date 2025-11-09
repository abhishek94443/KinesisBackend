package com.myapp.kinesis.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Global Cross-Origin Resource Sharing (CORS) configuration.
 * This file defines a bean that tells our backend which frontend domains
 * are allowed to make requests to our API.
 */
@Configuration
public class CorsConfig {

    /**
     * This bean is automatically injected into the SecurityFilterChain
     * in WebSecurityConfig.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 1. Define allowed origins (your frontend URLs)
        // We MUST be specific for a professional app.
        // We include "localhost" ports for local development.
        configuration.setAllowedOrigins(List.of(
                "http://localhost:3000", // Common for React
                "http://localhost:5173", // Common for Vite (React/Vue)
                "http://localhost:8081", // Common for Angular
                "https-admin.kinesis.io", // Your (future) admin PWA
                "https-app.kinesis.io"    // Your (future) customer PWA
        ));

        // 2. Define allowed HTTP methods
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

        // 3. Define allowed headers
        // We must allow "Authorization" (for our JWT) and "Content-Type".
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With"));

        // 4. Allow credentials (cookies, auth tokens)
        configuration.setAllowCredentials(true);

        // 5. Register this configuration for all /api/ paths
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);

        return source;
    }
}