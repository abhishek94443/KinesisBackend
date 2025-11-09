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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * v2 (Corrected): Stores the raw JWT token in the 'credentials'
 * field of the Authentication object. This allows our TenantInterceptor
 * to access the token without having to re-parse the header.
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

                UserDetails userDetails = this.staffUserDetailsService.loadUserByUsername(userEmail);

                if (jwtService.isTokenValid(jwt, userDetails)) {
                    Claims claims = jwtService.extractAllClaims(jwt);
                    String role = claims.get("role", String.class);

                    if (role != null && (role.equals("ROLE_VENDOR_OWNER") || role.equals("ROLE_STAFF") || role.equals("ROLE_SUPERADMIN"))) {

                        // THIS IS THE CORRECTION:
                        // We store the 'userDetails' as the principal
                        // We store the raw 'jwt' token as the credentials
                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                userDetails,
                                jwt, // <-- Store the token here
                                userDetails.getAuthorities()
                        );

                        authToken.setDetails(
                                new WebAuthenticationDetailsSource().buildDetails(request)
                        );

                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    } else {
                        logger.warn("JWT is valid but is not a Staff token. Denying access.");
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Cannot set staff user authentication", e);
        }

        filterChain.doFilter(request, response);
    }
}