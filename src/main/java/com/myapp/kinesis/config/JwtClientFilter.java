package com.myapp.kinesis.config;

import com.myapp.kinesis.modules.customer.service.ClientUserDetailsService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * This is the "Customer Portal Bouncer."
 * This filter runs on all non-admin routes.
 * It reads the JWT and validates it against the 'clients' table
 * *using the vendor context* provided in the token.
 */
@Component
public class JwtClientFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtClientFilter.class);

    private final JwtService jwtService;
    private final ClientUserDetailsService clientUserDetailsService;

    public JwtClientFilter(JwtService jwtService,
                           @Qualifier("clientUserDetailsService") ClientUserDetailsService clientUserDetailsService) {
        this.jwtService = jwtService;
        this.clientUserDetailsService = clientUserDetailsService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);

        try {
            userEmail = jwtService.extractUsername(jwt);

            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // --- This is the key "Air-Gap" logic ---
                Claims claims = jwtService.extractAllClaims(jwt);
                String role = claims.get("role", String.class);
                String vendorIdStr = claims.get("vendorId", String.class);

                // We ONLY proceed if this is a CLIENT token and has a vendorId
                if ("ROLE_CUSTOMER".equals(role) && vendorIdStr != null) {

                    UUID vendorId = UUID.fromString(vendorIdStr);

                    // Use the custom method to load from the 'clients' table
                    UserDetails userDetails = this.clientUserDetailsService
                            .loadClientByEmailAndVendor(userEmail, vendorId);

                    if (jwtService.isTokenValid(jwt, userDetails)) {

                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                userDetails,
                                jwt, // Store the token in the credentials
                                userDetails.getAuthorities() // This will be 'ROLE_CUSTOMER'
                        );

                        authToken.setDetails(
                                new WebAuthenticationDetailsSource().buildDetails(request)
                        );

                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    }
                } else {
                    logger.warn("JWT is valid but is not a Client token. Denying access.");
                }
            }
        } catch (Exception e) {
            // Correct, professional logging:
            logger.warn("Cannot set client user authentication", e);
        }

        filterChain.doFilter(request, response);
    }
}