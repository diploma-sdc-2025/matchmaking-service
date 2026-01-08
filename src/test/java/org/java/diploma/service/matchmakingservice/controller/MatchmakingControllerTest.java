package org.java.diploma.service.matchmakingservice.controller;

import org.java.diploma.service.matchmakingservice.dto.QueueJoinResponse;
import org.java.diploma.service.matchmakingservice.dto.QueueStatusResponse;
import org.java.diploma.service.matchmakingservice.service.MatchmakingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MatchmakingController.class)
@ActiveProfiles("test")
class MatchmakingControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    MatchmakingService matchmakingService;

    @Test
    @WithMockUser(username = "42")
    void joinEndpoint_shouldReturn200() throws Exception {
        QueueJoinResponse response = new QueueJoinResponse(
                "JOINED",
                42L,
                1L,
                Instant.now()
        );

        when(matchmakingService.joinQueue(any(Authentication.class)))
                .thenReturn(response);

        mockMvc.perform(
                        post("/api/matchmaking/join")
                                .with(csrf())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("JOINED"))
                .andExpect(jsonPath("$.userId").value(42));
    }

    @Test
    @WithMockUser(username = "42")
    void leaveEndpoint_shouldReturn200() throws Exception {
        mockMvc.perform(
                        post("/api/matchmaking/leave")
                                .with(csrf())
                )
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "42")
    void statusEndpoint_shouldReturnQueueStatus() throws Exception {
        when(matchmakingService.getQueueStatus(any(Authentication.class)))
                .thenReturn(new QueueStatusResponse(true, 1, 3));

        mockMvc.perform(get("/api/matchmaking/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inQueue").value(true))
                .andExpect(jsonPath("$.position").value(1))
                .andExpect(jsonPath("$.queueSize").value(3));
    }
}