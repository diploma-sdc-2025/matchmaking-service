package org.java.diploma.service.matchmakingservice.service;

import org.java.diploma.service.matchmakingservice.dto.QueueJoinResponse;
import org.java.diploma.service.matchmakingservice.dto.QueueStatusResponse;
import org.java.diploma.service.matchmakingservice.entity.MatchmakingHistory;
import org.java.diploma.service.matchmakingservice.repository.MatchmakingHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
@ActiveProfiles("test")
class MatchmakingServiceTest {

    @Mock
    RedisTemplate<String, Object> redisTemplate;

    @Mock
    ZSetOperations<String, Object> zSetOperations;

    @Mock
    RedisPublisher redisPublisher;

    @Mock
    MatchmakingHistoryRepository historyRepository;

    @InjectMocks
    MatchmakingService matchmakingService;

    @BeforeEach
    void setup() {
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);

        var auth =
                new UsernamePasswordAuthenticationToken("42", null);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void joinQueue_shouldAddUserAndPersistHistory() {
        when(zSetOperations.score(anyString(), anyString()))
                .thenReturn(null);
        when(zSetOperations.zCard("queue:active"))
                .thenReturn(1L);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        QueueJoinResponse response = matchmakingService.joinQueue(auth);

        assertNotNull(response);
        assertEquals("JOINED", response.status());
        assertEquals(42L, response.userId());

        verify(zSetOperations).add(eq("queue:active"), eq("42"), anyDouble());
        verify(historyRepository).save(any(MatchmakingHistory.class));
        verify(redisPublisher).publish(any());
    }


    @Test
    void joinQueue_shouldDoNothing_ifUserAlreadyInQueue() {
        when(zSetOperations.score("queue:active", "42"))
                .thenReturn(1.0);
        when(zSetOperations.zCard("queue:active"))
                .thenReturn(5L);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        QueueJoinResponse response = matchmakingService.joinQueue(auth);

        assertNotNull(response);
        assertEquals("ALREADY_IN_QUEUE", response.status());
        assertEquals(42L, response.userId());

        verify(zSetOperations, never()).add(any(), any(), anyDouble());
        verify(historyRepository, never()).save(any());
        verify(redisPublisher, never()).publish(any());
    }


    @Test
    void leaveQueue_shouldCancelWaitingHistory() {
        MatchmakingHistory history = new MatchmakingHistory();
        history.setUserId(42L);
        history.setJoinedAt(Instant.now().minusSeconds(10));
        history.setStatus("WAITING");

        when(historyRepository.findFirstByUserIdAndStatus(42L, "WAITING"))
                .thenReturn(Optional.of(history));

        matchmakingService.leaveQueue();

        assertEquals("CANCELLED", history.getStatus());
        assertNotNull(history.getLeftQueueAt());
        assertTrue(history.getWaitTimeSeconds() >= 10);

        verify(zSetOperations).remove("queue:active", "42");
        verify(historyRepository).save(history);
        verify(redisPublisher).publish(any());
    }

    @Test
    void getQueueStatus_shouldReturnCorrectPosition() {
        when(zSetOperations.rank("queue:active", "42"))
                .thenReturn(1L);
        when(zSetOperations.zCard("queue:active"))
                .thenReturn(5L);

        QueueStatusResponse response =
                matchmakingService.getQueueStatus();

        assertTrue(response.isInQueue());
        assertEquals(2, response.getPosition());
        assertEquals(5, response.getQueueSize());
    }

    @Test
    void getQueueStatus_shouldHandleUserNotInQueue() {
        when(zSetOperations.rank("queue:active", "42"))
                .thenReturn(null);
        when(zSetOperations.zCard("queue:active"))
                .thenReturn(3L);

        QueueStatusResponse response =
                matchmakingService.getQueueStatus();

        assertFalse(response.isInQueue());
        assertNull(response.getPosition());
        assertEquals(3, response.getQueueSize());
    }
}
