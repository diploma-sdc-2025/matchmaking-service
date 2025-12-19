package org.java.diploma.service.matchmakingservice.integration;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.crypto.SecretKey;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@EnabledIfDockerAvailable
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test")
class MatchmakingIntegrationTest {

    @Container
    static GenericContainer<?> redis =
            new GenericContainer<>("redis:7-alpine")
                    .withExposedPorts(6379);

    @Autowired
    MockMvc mockMvc;

    private String jwtToken;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", () -> redis.getMappedPort(6379));
        registry.add(
                "auth.jwt.secret",
                () -> "test-secret-test-secret-test-secret-test-secret"
        );
    }

    @BeforeAll
    void setupJwt() {
        SecretKey key = Keys.hmacShaKeyFor(
                "test-secret-test-secret-test-secret-test-secret".getBytes()
        );

        jwtToken = Jwts.builder()
                .setSubject("42")
                .signWith(key)
                .compact();
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
