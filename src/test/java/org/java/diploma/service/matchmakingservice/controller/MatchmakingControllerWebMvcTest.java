package org.java.diploma.service.matchmakingservice.controller;

import org.java.diploma.service.matchmakingservice.security.JwtUserIdFilter;
import org.java.diploma.service.matchmakingservice.service.MatchQueueService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MatchmakingController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = "auth.jwt.secret=very-secure-test-key-that-is-at-least-32-bytes")
class MatchmakingControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MatchQueueService queueService;

    @Test
    void joinReturnsQueuePayload() throws Exception {
        when(queueService.join(42L)).thenReturn(new MatchQueueService.JoinResult("queued", 42L, 3, null));

        mockMvc.perform(post("/api/matchmaking/join")
                        .requestAttr(JwtUserIdFilter.ATTR_USER_ID, 42L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("queued"))
                .andExpect(jsonPath("$.userId").value(42))
                .andExpect(jsonPath("$.queueSize").value(3));

        verify(queueService).invalidateAssignmentIfStale(42L);
        verify(queueService).join(42L);
    }

    @Test
    void leaveReturnsNoContent() throws Exception {
        mockMvc.perform(post("/api/matchmaking/leave")
                        .requestAttr(JwtUserIdFilter.ATTR_USER_ID, 12L))
                .andExpect(status().isNoContent());

        verify(queueService).leave(12L);
    }

    @Test
    void statusReturnsMatchIdWhenAssigned() throws Exception {
        when(queueService.status(7L)).thenReturn(new MatchQueueService.StatusResult(false, null, 0, 991));

        mockMvc.perform(get("/api/matchmaking/status")
                        .requestAttr(JwtUserIdFilter.ATTR_USER_ID, 7L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inQueue").value(false))
                .andExpect(jsonPath("$.queueSize").value(0))
                .andExpect(jsonPath("$.matchId").value(991));
    }
}
