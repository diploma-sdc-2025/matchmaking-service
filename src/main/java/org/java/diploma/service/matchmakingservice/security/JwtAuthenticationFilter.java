package org.java.diploma.service.matchmakingservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private static final String HEADER_AUTHORIZATION = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private static final String JWT_VALIDATED_SUCCESS = "JWT token validated successfully for user: {}";
    private static final String JWT_TOKEN_EXPIRED = "JWT token expired for request to {}";
    private static final String JWT_TOKEN_INVALID = "Invalid JWT token for request to {}: {}";
    private static final String JWT_USERID_INVALID_FORMAT = "Invalid userId format in JWT token: {}";
    private static final String JWT_VALIDATION_UNEXPECTED_ERROR = "Unexpected error during JWT validation for request to {}";
    private static final String NO_JWT_TOKEN_FOUND = "No valid JWT token found in Authorization header for request to {}";

    private final SecretKey key;

    public JwtAuthenticationFilter(@Value("${auth.jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        logger.info("JwtAuthenticationFilter initialized");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String header = request.getHeader(HEADER_AUTHORIZATION);
        String requestUri = request.getRequestURI();

        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            logger.debug(NO_JWT_TOKEN_FOUND, requestUri);
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String token = header.substring(BEARER_PREFIX.length());

            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String userIdString = claims.getSubject();
            Long userId = Long.parseLong(userIdString);

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(
                            userId,
                            null,
                            Collections.emptyList()
                    );

            SecurityContextHolder.getContext().setAuthentication(auth);

            logger.debug(JWT_VALIDATED_SUCCESS, userId);

        } catch (ExpiredJwtException ex) {
            logger.warn(JWT_TOKEN_EXPIRED, requestUri);
            SecurityContextHolder.clearContext();
        } catch (MalformedJwtException | SignatureException ex) {
            logger.warn(JWT_TOKEN_INVALID, requestUri, ex.getClass().getSimpleName());
            SecurityContextHolder.clearContext();
        } catch (NumberFormatException ex) {
            logger.error(JWT_USERID_INVALID_FORMAT, ex.getMessage());
            SecurityContextHolder.clearContext();
        } catch (Exception ex) {
            logger.error(JWT_VALIDATION_UNEXPECTED_ERROR, requestUri, ex);
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}