package com.myapp.kinesis.config;

import com.myapp.kinesis.modules.customer.service.ClientUserDetailsService;
import com.myapp.kinesis.modules.staff.service.StaffUserDetailsService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * v5 (Hardened): Implements reviewer feedback.
 * - (Fix 2C) OpenAPI/Swagger endpoints are now only public in the 'dev' profile.
 * - (Fix 2D) PasswordEncoder strength is now configurable from application.properties.
 * - (REJECTED A) We are *keeping* the non-deprecated DaoAuthenticationProvider(UserDetailsService) constructor.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class WebSecurityConfig {

    // (Fix 2C) Inject the Environment to check which profile is active
    private final Environment environment;

    public WebSecurityConfig(Environment environment) {
        this.environment = environment;
    }

    // --- SHARED BEANS ---

    /**
     * (Fix 2D) PasswordEncoder strength is now injected from properties.
     */
    @Bean
    public PasswordEncoder passwordEncoder(
            @Value("${security.bcrypt.strength:11}") int strength
    ) {
        return new BCryptPasswordEncoder(strength);
    }

    // --- 1. ADMIN/STAFF SECURITY CHAIN (High-Security) ---

    @Bean
    public AuthenticationProvider staffAuthenticationProvider(
            @Qualifier("staffUserDetailsService") StaffUserDetailsService staffUserDetailsService,
            PasswordEncoder passwordEncoder
    ) {
        // This is the correct, non-deprecated constructor pattern.
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
                .authorizeHttpRequests(authz -> {

                    authz.requestMatchers("/api/auth/admin/**").permitAll();

                    // (Fix 2C) Only allow Swagger for the admin chain if "dev" profile is active
                    configureSwagger(authz, "dev");

                    authz.anyRequest().authenticated();
                })
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
                .authorizeHttpRequests(authz -> {

                    authz.requestMatchers("/api/public/**").permitAll();
                    authz.requestMatchers("/api/auth/customer/**").permitAll();

                    // (Fix 2C) Also configure Swagger for the public chain
                    configureSwagger(authz, "dev");

                    authz.anyRequest().authenticated();
                })
                .authenticationProvider(clientAuthenticationProvider)
                .addFilterBefore(jwtClientFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * (Fix 2C) Helper method to only permit Swagger access
     * if the specified profile (e.g., "dev") is active.
     */
    private void configureSwagger(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry authz,
            String profile) {

        if (environment.acceptsProfiles(Profiles.of(profile))) {
            authz.requestMatchers(
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html"
            ).permitAll();
        }
    }
}