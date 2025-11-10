package com.myapp.kinesis.config;

import com.myapp.kinesis.modules.staff.service.StaffUserDetailsService;
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

/**
 * v3 (Corrected):
 * This filter now correctly reads the 'role' claim FROM THE JWT
 * and uses that to build the user's authorities. This is the
 * core of our "Contextual Authorization" and fixes the 403 bug.
 */
@Component
public class JwtStaffFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtStaffFilter.class);

    private final JwtService jwtService;
    private final StaffUserDetailsService staffUserDetailsService;

    public JwtStaffFilter(JwtService jwtService,
                          @Qualifier("staffUserDetailsService") StaffUserDetailsService staffUserDetailsService) {
        this.jwtService = jwtService;
        this.staffUserDetailsService = staffUserDetailsService;
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

                UserDetails userDetails = this.staffUserDetailsService.loadUserByUsername(userEmail);

                if (jwtService.isTokenValid(jwt, userDetails)) {
                    // --- THIS IS THE CRITICAL FIX ---
                    Claims claims = jwtService.extractAllClaims(jwt);
                    String role = claims.get("role", String.class);

                    // We must ensure this is a Staff-type token
                    if (role != null && role.startsWith("ROLE_")) {

                        // 1. Create the authority from the role IN THE TOKEN
                        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(role));

                        // 2. Create the auth token with the *correct* contextual role
                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                userDetails,
                                jwt, // Store the token in the credentials for the interceptor
                                authorities // Use the role from the token
                        );

                        authToken.setDetails(
                                new WebAuthenticationDetailsSource().buildDetails(request)
                        );

                        // 3. Set the user in the SecurityContext.
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    } else {
                        logger.warn("JWT is valid but is missing a valid 'role' claim. Denying access.");
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Cannot set staff user authentication", e);
        }

        filterChain.doFilter(request, response);
    }
}