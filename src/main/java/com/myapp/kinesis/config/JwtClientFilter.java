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
 * v3 (Corrected):
 * This filter now correctly reads the 'role' claim FROM THE JWT
 * to build the user's authorities.
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

                // We ONLY proceed if this is a CLIENT token
                if ("ROLE_CUSTOMER".equals(role) && vendorIdStr != null) {

                    UUID vendorId = UUID.fromString(vendorIdStr);

                    UserDetails userDetails = this.clientUserDetailsService
                            .loadClientByEmailAndVendor(userEmail, vendorId);

                    if (jwtService.isTokenValid(jwt, userDetails)) {

                        // 1. Create the authority from the role IN THE TOKEN
                        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(role));

                        // 2. Create the auth token with the *correct* contextual role
                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                userDetails,
                                jwt, // Store the token in the credentials
                                authorities // Use the role from the token
                        );

                        authToken.setDetails(
                                new WebAuthenticationDetailsSource().buildDetails(request)
                        );

                        // 3. Set the user in the SecurityContext.
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    }
                } else {
                    logger.warn("JWT is valid but is not a Client token. Denying access.");
                }
            }
        } catch (Exception e) {
            logger.warn("Cannot set client user authentication", e);
        }

        filterChain.doFilter(request, response);
    }
}