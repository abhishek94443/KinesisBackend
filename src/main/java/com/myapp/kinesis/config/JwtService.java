package com.myapp.kinesis.config;

import com.myapp.kinesis.common.exceptions.JwtTokenExpiredException;
import com.myapp.kinesis.common.exceptions.JwtTokenValidationException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * v3 (Hardened): Implements reviewer feedback.
 * - Adds explicit HS256 algorithm.
 * - Adds 'jti' (JWT ID) and 'iss' (Issuer) claims.
 * - Adds robust exception handling for parsing.
 * - Adds a startup check for JWT secret strength.
 * - (THIS FIX) Adds 60-second clock skew tolerance.
 */
@Service
public class JwtService {

    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationInMs;

    private SecretKey signingKey;

    private static final String ISSUER = "kinesis-api";
    private static final int MIN_SECRET_LENGTH_BYTES = 32; // 256 bits for HS256
    private static final long CLOCK_SKEW_SECONDS = 60; // 1 minute grace period

    /**
     * This method runs *after* @Value injection.
     * It validates our secret key strength. We will fail-fast.
     */
    @PostConstruct
    public void init() {
        byte[] keyBytes = this.jwtSecret.getBytes(StandardCharsets.UTF_8);

        if (keyBytes.length < MIN_SECRET_LENGTH_BYTES) {
            logger.error("FATAL: JWT secret is too weak! Must be at least {} bytes (256 bits) for HS256.", MIN_SECRET_LENGTH_BYTES);
            throw new IllegalStateException("JWT secret is not secure. Application startup failed.");
        }

        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        logger.info("JWT Service initialized with HS256 algorithm.");
    }

    // --- Token Generation ---

    public String generateToken(Map<String, Object> claims, UserDetails userDetails) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);

        return Jwts.builder()
                .claims(claims)
                .subject(userDetails.getUsername())
                .issuedAt(now)
                .expiration(expiryDate)
                .id(UUID.randomUUID().toString())      // jti: JWT ID
                .issuer(ISSUER)                      // iss: Who issued this
                .signWith(signingKey, SignatureAlgorithm.HS256) // Explicit algorithm
                .compact();
    }

    // --- Token Validation & Parsing ---

    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            // We still check isTokenExpired for an extra layer of logic,
            // but the parser's clock skew will handle the validation.
            return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
        } catch (JwtTokenValidationException | JwtTokenExpiredException e) {
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        // We add our *own* clock skew check here to be safe
        // (This check is now less critical as the parser handles it,
        // but it's good defense-in-depth)
        Date expiration = extractExpiration(token);
        Date nowWithSkew = new Date(System.currentTimeMillis() - (CLOCK_SKEW_SECONDS * 1000));
        return expiration.before(nowWithSkew);
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public Claims extractAllClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(signingKey)
                    // --- THIS IS THE FIX ---
                    // Allow for a 60-second clock difference
                    // between the server that *issued* the token
                    // and this server (the *validator*).
                    .clockSkewSeconds(CLOCK_SKEW_SECONDS)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            logger.warn("JWT token has expired: {}", e.getMessage());
            throw new JwtTokenExpiredException("JWT token has expired", e);
        } catch (JwtException e) {
            logger.warn("Invalid JWT token: {}", e.getMessage());
            throw new JwtTokenValidationException("Invalid JWT token: " + e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            logger.warn("JWT claims string is empty: {}", e.getMessage());
            throw new JwtTokenValidationException("Invalid JWT token: claims empty", e);
        }
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }
}