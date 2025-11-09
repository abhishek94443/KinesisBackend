package com.myapp.kinesis.tenant;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * This configuration class registers our TenantInterceptor with
 * Spring's request-handling system (WebMvcConfigurer).
 */
@Configuration
public class TenantConfig implements WebMvcConfigurer {

    private final TenantInterceptor tenantInterceptor;

    public TenantConfig(TenantInterceptor tenantInterceptor) {
        this.tenantInterceptor = tenantInterceptor;
    }

    /**
     * We add our interceptor to the registry and tell it which
     * paths it should (and should not) apply to.
     */
    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry
                // Add our interceptor
                .addInterceptor(tenantInterceptor)

                // Apply it to all secure API routes
                .addPathPatterns("/api/admin/**", "/api/customer/**")

                // --- IMPORTANT ---
                // EXCLUDE all public and auth routes. We don't need
                // tenant context for login, registration, or public service lists.
                .excludePathPatterns(
                        "/api/auth/**",
                        "/api/public/**",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html"
                );
    }
}