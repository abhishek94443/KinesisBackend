package com.myapp.kinesis.config;

import com.myapp.kinesis.modules.customer.service.ClientUserDetailsService;
import com.myapp.kinesis.modules.staff.service.StaffUserDetailsService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * v5 (Corrected):
 * REMOVED the global AuthenticationManager bean.
 * Each AuthService will now inject its *specific* AuthenticationProvider
 * to enforce the "Air-Gap" and prevent provider conflicts.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class WebSecurityConfig {

    // --- SHARED BEANS ---

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // --- 1. ADMIN/STAFF SECURITY CHAIN (High-Security) ---

    @Bean
    public AuthenticationProvider staffAuthenticationProvider(
            @Qualifier("staffUserDetailsService") StaffUserDetailsService staffUserDetailsService,
            PasswordEncoder passwordEncoder
    ) {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(staffUserDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }

    @Bean
    @Order(1)
    public SecurityFilterChain staffSecurityFilterChain(
            HttpSecurity http,
            JwtStaffFilter jwtStaffFilter,
            CorsConfigurationSource corsConfigurationSource,
            @Qualifier("staffAuthenticationProvider") AuthenticationProvider staffAuthenticationProvider
    ) throws Exception {

        http
                .securityMatcher("/api/admin/**", "/api/auth/admin/**")
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers("/api/auth/admin/**").permitAll()
                        .anyRequest().authenticated()
                )
                .authenticationProvider(staffAuthenticationProvider)
                .addFilterBefore(jwtStaffFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // --- 2. CLIENT/CUSTOMER SECURITY CHAIN (Low-Security) ---

    @Bean
    public AuthenticationProvider clientAuthenticationProvider(
            @Qualifier("clientUserDetailsService") ClientUserDetailsService clientUserDetailsService,
            PasswordEncoder passwordEncoder
    ) {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(clientUserDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }

    @Bean
    @Order(2)
    public SecurityFilterChain clientSecurityFilterChain(
            HttpSecurity http,
            JwtClientFilter jwtClientFilter,
            CorsConfigurationSource corsConfigurationSource,
            @Qualifier("clientAuthenticationProvider") AuthenticationProvider clientAuthenticationProvider
    ) throws Exception {

        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers("/api/auth/customer/**").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .anyRequest().authenticated()
                )
                .authenticationProvider(clientAuthenticationProvider)
                .addFilterBefore(jwtClientFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}