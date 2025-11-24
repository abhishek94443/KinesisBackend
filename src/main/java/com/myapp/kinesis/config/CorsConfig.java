package com.myapp.kinesis.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // CHANGE 1: Use setAllowedOriginPatterns("*")
        // This is the magic fix. It allows requests from ANY source during dev
        // but still supports 'AllowCredentials=true'.
        configuration.setAllowedOriginPatterns(List.of("*"));

        // CHANGE 2: Explicitly allow all standard methods
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

        // CHANGE 3: Allow all headers (Authorization is the key one)
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With", "Accept", "Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers"));

        // CHANGE 4: Expose headers so the frontend can see them
        configuration.setExposedHeaders(List.of("Access-Control-Allow-Origin", "Access-Control-Allow-Credentials"));

        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Apply to ALL routes, not just /api/**, to be safe
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}