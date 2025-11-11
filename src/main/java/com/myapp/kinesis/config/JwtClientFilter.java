package com.myapp.kinesis.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myapp.kinesis.common.dto.ApiResponse;
import com.myapp.kinesis.common.exceptions.JwtTokenExpiredException;
import com.myapp.kinesis.common.exceptions.JwtTokenValidationException;
import com.myapp.kinesis.modules.customer.service.ClientUserDetailsService;
import com.myapp.kinesis.tenant.TenantContext;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * v5 (Hardened):
 * - (Fix 2E) Adds debug logging.
 * - (Fix 3) Injects and SETS the TenantContext "backpack".
 * - (Fix 3) REMOVES the dangerous RLS logic.
 */
@Component
public class JwtClientFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtClientFilter.class);

    private final JwtService jwtService;
    private final ClientUserDetailsService clientUserDetailsService;
    private final ObjectMapper objectMapper;
    private final TenantContext tenantContext; // (Fix 3) The "backpack"

    public JwtClientFilter(JwtService jwtService,
                           @Qualifier("clientUserDetailsService") ClientUserDetailsService clientUserDetailsService,
                           ObjectMapper objectMapper,
                           TenantContext tenantContext) { // (Fix 3)
        this.jwtService = jwtService;
        this.clientUserDetailsService = clientUserDetailsService;
        this.objectMapper = objectMapper;
        this.tenantContext = tenantContext;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        logger.debug("JwtClientFilter is running for request: {}", request.getRequestURI());

        final String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7);
        final String userEmail;

        try {
            userEmail = jwtService.extractUsername(jwt);

            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                Claims claims = jwtService.extractAllClaims(jwt);
                String role = claims.get("role", String.class);
                String vendorIdStr = claims.get("vendorId", String.class);

                if ("ROLE_CUSTOMER".equals(role) && vendorIdStr != null) {

                    UUID vendorId = UUID.fromString(vendorIdStr);

                    UserDetails userDetails = this.clientUserDetailsService
                            .loadClientByEmailAndVendor(userEmail, vendorId);

                    if (jwtService.isTokenValid(jwt, userDetails)) {

                        // --- (Fix 3) SET THE TENANT "BACKPACK" ---
                        tenantContext.setVendorId(vendorId);

                        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(role));
                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                userDetails, jwt, authorities
                        );
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    }
                }
            }

            filterChain.doFilter(request, response);

        } catch (JwtTokenExpiredException | JwtTokenValidationException e) {
            logger.warn("JWT validation failed: {}", e.getMessage());
            sendErrorResponse(response, HttpStatus.UNAUTHORIZED, e.getMessage());
            return;

        } catch (Exception e) {
            logger.warn("JWT authentication processing error", e);
            sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "Authentication failed.");
            return;
        } finally {
            // --- (Fix 3) CRITICAL: CLEAR THE "BACKPACK" ---
            tenantContext.clear();
        }
    }

    private void sendErrorResponse(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiResponse<?> apiResponse = ApiResponse.error(message);
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }
}