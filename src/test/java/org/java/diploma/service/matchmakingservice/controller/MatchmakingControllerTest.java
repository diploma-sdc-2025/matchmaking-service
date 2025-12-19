package org.java.diploma.service.matchmakingservice.controller;

import org.java.diploma.service.matchmakingservice.dto.QueueStatusResponse;
import org.java.diploma.service.matchmakingservice.service.MatchmakingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import org.springframework.security.test.context.support.WithMockUser;

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
        mockMvc.perform(
                        post("/api/matchmaking/join")
                                .with(csrf())
                )
                .andExpect(status().isOk());
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
        when(matchmakingService.getQueueStatus())
                .thenReturn(new QueueStatusResponse(true, 1, 3));

        mockMvc.perform(get("/api/matchmaking/status"))
                .andExpect(status().isOk());
    }
}


