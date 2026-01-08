package org.java.diploma.service.matchmakingservice.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import javax.crypto.SecretKey;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
class JwtAuthenticationFilterTest {

    @Test
    void shouldAuthenticateValidJwt() throws Exception {
        String secret = "test-secret-test-secret-test-secret";
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes());

        String token = Jwts.builder()
                .setSubject("42")
                .signWith(key)
                .compact();

        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(secret);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);

        filter.doFilter(
                request,
                new MockHttpServletResponse(),
                (req, res) -> {}
        );

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());

        assertEquals(
                42L,
                SecurityContextHolder.getContext()
                        .getAuthentication()
                        .getPrincipal()
        );
    }
}