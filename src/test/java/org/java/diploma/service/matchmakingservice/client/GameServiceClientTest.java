package org.java.diploma.service.matchmakingservice.client;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameServiceClientTest {

    private MockWebServer server;
    private GameServiceClient client;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        client = new GameServiceClient(server.url("/").toString());
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void createMatchReturnsMatchId() {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("{\"matchId\":123}"));

        int matchId = client.createMatch(List.of(10L, 20L));

        assertEquals(123, matchId);
    }

    @Test
    void createMatchThrowsOnServerError() {
        server.enqueue(new MockResponse().setResponseCode(500).setBody("boom"));

        assertThrows(IllegalStateException.class, () -> client.createMatch(List.of(10L, 20L)));
    }

    @Test
    void matchContainsPlayerReturnsTrueWhenPresent() {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("{\"matchId\":123,\"players\":[10,20]}"));

        assertTrue(client.matchContainsPlayer(123, 20L));
    }

    @Test
    void matchContainsPlayerReturnsFalseWhenServiceUnavailable() {
        server.enqueue(new MockResponse().setResponseCode(503));

        assertTrue(client.matchContainsPlayer(123, 20L));
    }
}
