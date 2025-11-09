package com.myapp.kinesis.tenant;

import com.myapp.kinesis.config.JwtService;
import com.myapp.kinesis.modules.customer.entity.ClientEntity;
import com.myapp.kinesis.modules.staff.entity.StaffEntity;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

/**
 * This interceptor is the core of our "Defense in Depth" security.
 * It runs *after* the JWT filters (which set the Authentication)
 * and *before* the controller (which runs the business logic).
 * <p>
 * It is responsible for setting BOTH:
 * 1. The Java-level TenantContext (ThreadLocal "backpack") for our ALT.
 * 2. The Database-level session variable ('app.current_user_id') for our RLS.
 */
@Component
public class TenantInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(TenantInterceptor.class);

    private final JwtService jwtService;
    private final TenantContext tenantContext;
    private final JdbcTemplate jdbcTemplate; // For setting RLS

    public TenantInterceptor(JwtService jwtService, TenantContext tenantContext, JdbcTemplate jdbcTemplate) {
        this.jwtService = jwtService;
        this.tenantContext = tenantContext;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * This runs *before* the controller method.
     */
    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) throws Exception {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Check if user is authenticated (i.e., not a public route)
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            // This is a public request (e.g., /api/public/**).
            // No tenant context is needed. We let it pass.
            return true;
        }

        // --- This is a SECURE request ---
        String token = extractJwtFromRequest(request);
        if (token == null) {
            // This should be impossible if Spring Security is working, but a good safety check.
            logger.warn("Authenticated request is missing JWT token.");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing credentials.");
            return false;
        }

        try {
            Claims claims = jwtService.extractAllClaims(token);
            String vendorIdStr = claims.get("vendorId", String.class);
            String role = claims.get("role", String.class);

            // Get the user's *global* ID from their UserDetails object
            // This is the correct way to get the ID (Staff ID or Client ID)
            UUID userId = getUserIdFromPrincipal(authentication.getPrincipal(), role);

            if (vendorIdStr != null && userId != null) {
                UUID vendorId = UUID.fromString(vendorIdStr);

                // --- ACTION 1: Set the Java "Backpack" (ALT) ---
                tenantContext.setVendorId(vendorId);

                // --- ACTION 2: Set the Database "Safety Net" (RLS) ---
                // We will create RLS rules based on 'app.current_user_id'
                String rlsSql = "SET app.current_user_id = '" + userId.toString() + "'";
                jdbcTemplate.execute(rlsSql);
                logger.debug("Set RLS/ALT context for user {} at vendor {}", userId, vendorId);

            } else {
                // This can happen for a SUPERADMIN who has no vendorId.
                if ("ROLE_SUPERADMIN".equals(role)) {
                    logger.debug("SUPERADMIN request. Skipping tenant context.");
                } else {
                    logger.warn("Authenticated token is missing 'vendorId' or 'userId' claims.");
                }
            }

        } catch (Exception e) {
            logger.error("Error setting tenant context from JWT", e);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token claims.");
            return false;
        }

        // Continue the request to the controller
        return true;
    }

    /**
     * This runs *after* the controller has finished.
     * This is the "leak-proof" guarantee.
     */
    @Override
    public void afterCompletion(@NonNull HttpServletRequest request,
                                @NonNull HttpServletResponse response,
                                @NonNull Object handler,
                                Exception ex) throws Exception {

        // --- THIS IS THE CRITICAL "LEAK-PROOF" STEP ---
        // This 'afterCompletion' block runs *after* the request is
        // finished, even if the controller threw an exception.

        // 1. Clear the ThreadLocal "backpack"
        tenantContext.clear();

        // 2. Reset the database session variable
        try {
            jdbcTemplate.execute("RESET app.current_user_id");
        } catch (Exception e) {
            // We log this, but don't re-throw, as the request is already over.
            logger.error("Failed to reset RLS 'app.current_user_id'", e);
        }
    }

    // Helper to extract the token from the header
    private String extractJwtFromRequest(HttpServletRequest request) {
        final String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        return authHeader.substring(7);
    }

    // Helper to get the ID from either StaffEntity or ClientEntity
    private UUID getUserIdFromPrincipal(Object principal, String role) {
        if ("ROLE_CUSTOMER".equals(role)) {
            if (principal instanceof ClientEntity) {
                return ((ClientEntity) principal).getId();
            }
        } else { // VENDOR_OWNER, STAFF, SUPERADMIN
            if (principal instanceof StaffEntity) {
                return ((StaffEntity) principal).getId();
            }
        }
        logger.warn("Unknown principal/role combination: {} / {}", principal.getClass().getName(), role);
        return null;
    }
}