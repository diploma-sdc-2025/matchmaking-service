package org.java.diploma.service.matchmakingservice.service;

import org.java.diploma.service.matchmakingservice.client.GameServiceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchQueueServiceTest {

    @Mock
    private StringRedisTemplate redis;
    @Mock
    private GameServiceClient gameServiceClient;
    @Mock
    private AnalyticsEventPublisher analyticsEventPublisher;
    @Mock
    private ListOperations<String, String> listOps;
    @Mock
    private ValueOperations<String, String> valueOps;

    private MatchQueueService service;

    @BeforeEach
    void setUp() {
        lenient().when(redis.opsForList()).thenReturn(listOps);
        lenient().when(redis.opsForValue()).thenReturn(valueOps);
        service = new MatchQueueService(redis, gameServiceClient, analyticsEventPublisher);
    }

    @Test
    void joinReturnsQueuedWhenNoImmediatePair() {
        when(listOps.size("matchmaking:queue")).thenReturn(1L);
        when(valueOps.get("matchmaking:assigned:15")).thenReturn(null);
        when(listOps.range("matchmaking:queue", 0, -1)).thenReturn(List.of("15"));

        MatchQueueService.JoinResult result = service.join(15L);

        assertEquals("queued", result.status());
        assertEquals(15L, result.userId());
        assertEquals(1, result.queueSize());
        assertNull(result.matchId());

        verify(redis).delete("matchmaking:assigned:15");
        verify(listOps).remove("matchmaking:queue", 0, "15");
        verify(listOps).rightPush("matchmaking:queue", "15");
    }

    @Test
    void joinReturnsMatchedAndStoresAssignmentsWhenPairingSucceeds() {
        when(listOps.size("matchmaking:queue")).thenReturn(2L, 1L, 0L);
        when(listOps.leftPop("matchmaking:queue")).thenReturn("20", "10");
        when(gameServiceClient.createMatch(any())).thenReturn(991);
        when(valueOps.get("matchmaking:assigned:10")).thenReturn("991");

        MatchQueueService.JoinResult result = service.join(10L);

        assertEquals("matched", result.status());
        assertEquals(991, result.matchId());

        ArgumentCaptor<List<Long>> playersCaptor = ArgumentCaptor.forClass(List.class);
        verify(gameServiceClient).createMatch(playersCaptor.capture());
        assertEquals(List.of(10L, 20L), playersCaptor.getValue());

        verify(valueOps).set(eq("matchmaking:assigned:10"), eq("991"), eq(Duration.ofHours(24)));
        verify(valueOps).set(eq("matchmaking:assigned:20"), eq("991"), eq(Duration.ofHours(24)));
    }

    @Test
    void statusReturnsQueuePositionWhenUserWaiting() {
        when(valueOps.get("matchmaking:assigned:7")).thenReturn(null);
        when(listOps.range("matchmaking:queue", 0, -1)).thenReturn(List.of("5", "7", "11"));

        MatchQueueService.StatusResult result = service.status(7L);

        assertEquals(true, result.inQueue());
        assertEquals(2, result.position());
        assertEquals(3, result.queueSize());
        assertNull(result.matchId());
    }

    @Test
    void invalidateAssignmentDropsMalformedMatchId() {
        when(valueOps.get("matchmaking:assigned:7")).thenReturn("bad-id");

        service.invalidateAssignmentIfStale(7L);

        verify(redis).delete("matchmaking:assigned:7");
    }

    @Test
    void purgeRemovesQueueAndAssignmentKeys() {
        when(redis.keys("matchmaking:assigned:*")).thenReturn(Set.of("matchmaking:assigned:1", "matchmaking:assigned:2"));

        int removed = service.purgeAllMatchmakingKeys();

        assertEquals(2, removed);
        verify(redis).delete(Set.of("matchmaking:assigned:1", "matchmaking:assigned:2"));
        verify(redis).delete("matchmaking:queue");
    }

    @Test
    void leaveRemovesUserFromQueue() {
        service.leave(99L);
        verify(listOps, times(1)).remove("matchmaking:queue", 0, "99");
    }
}
