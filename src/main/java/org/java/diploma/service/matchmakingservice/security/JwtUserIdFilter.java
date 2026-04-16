package org.java.diploma.service.matchmakingservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Resolves {@code userId} from JWT subject for {@code /api/matchmaking/**} (except OPTIONS).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class JwtUserIdFilter extends OncePerRequestFilter {

    public static final String ATTR_USER_ID = "matchmakingUserId";

    private static final String BEARER = "Bearer ";
    private final SecretKey key;
    private final Environment environment;

    public JwtUserIdFilter(@Value("${auth.jwt.secret}") String secret, Environment environment) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.environment = environment;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (environment.acceptsProfiles(Profiles.of("dev", "local"))
                && uri.startsWith("/api/matchmaking/dev/")) {
            return true;
        }
        if (!uri.startsWith("/api/matchmaking/")) {
            return true;
        }
        return HttpMethod.OPTIONS.matches(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER)) {
            unauthorized(response, "Missing or invalid Authorization header");
            return;
        }
        try {
            String token = header.substring(BEARER.length());
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            String sub = claims.getSubject();
            long userId = Long.parseLong(sub);
            request.setAttribute(ATTR_USER_ID, userId);
        } catch (Exception e) {
            unauthorized(response, "Invalid or expired token");
            return;
        }
        chain.doFilter(request, response);
    }

    private static void unauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"message\":\"" + message.replace("\"", "'") + "\"}");
    }
}
