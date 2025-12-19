package org.java.diploma.service.matchmakingservice.integration;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.ArgumentMatchers.any;

@SpringBootTest(
        properties = {
                "spring.data.redis.repositories.enabled=false"
        }
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MatchmakingIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    private RedisTemplate<String, Object> redisTemplate;

    @MockitoBean
    private StringRedisTemplate stringRedisTemplate;

    private String jwtToken;

    @BeforeEach
    void setupRedisMocks() {
        ValueOperations<String, Object> valueOps = mock(ValueOperations.class);
        ZSetOperations<String, Object> zSetOps = mock(ZSetOperations.class);

        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOps);

        when(zSetOps.score(any(), any())).thenReturn(null);
        when(zSetOps.zCard(any())).thenReturn(0L);

        when(stringRedisTemplate.opsForValue()).thenReturn(mock(ValueOperations.class));
    }

    @BeforeAll
    void setupJwt() {
        String secret = "test-secret-test-secret-test-secret-test-secret";
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes());

        jwtToken = Jwts.builder()
                .setSubject("42")
                .signWith(key)
                .compact();

        System.setProperty("auth.jwt.secret", secret);
    }

    @Test
    void fullFlow_join_status_leave() throws Exception {

        mockMvc.perform(
                post("/api/matchmaking/join")
                        .with(csrf())
                        .header("Authorization", "Bearer " + jwtToken)
        ).andExpect(status().isOk());

        mockMvc.perform(
                get("/api/matchmaking/status")
                        .header("Authorization", "Bearer " + jwtToken)
        ).andExpect(status().isOk());

        mockMvc.perform(
                post("/api/matchmaking/leave")
                        .with(csrf())
                        .header("Authorization", "Bearer " + jwtToken)
        ).andExpect(status().isOk());
    }
}
