package org.java.diploma.service.matchmakingservice.service;

import org.java.diploma.service.matchmakingservice.client.GameServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class MatchQueueService {

    private static final Logger log = LoggerFactory.getLogger(MatchQueueService.class);

    /** Nominal Elo for guest accounts when pairing uses ratings (queue is currently FIFO only). */
    @SuppressWarnings("unused")
    private static final int GUEST_EQUIVALENT_ELO = 1000;

    private static final String QUEUE_KEY = "matchmaking:queue";
    private static final String ASSIGNED_PREFIX = "matchmaking:assigned:";
    private static final Duration ASSIGNMENT_TTL = Duration.ofHours(24);

    private final StringRedisTemplate redis;
    private final GameServiceClient gameServiceClient;
    private final AnalyticsEventPublisher analyticsEventPublisher;
    private final Object pairLock = new Object();

    public MatchQueueService(
            StringRedisTemplate redis,
            GameServiceClient gameServiceClient,
            AnalyticsEventPublisher analyticsEventPublisher
    ) {
        this.redis = redis;
        this.gameServiceClient = gameServiceClient;
        this.analyticsEventPublisher = analyticsEventPublisher;
    }

    /**
     * Clears any stale assignment, re-queues the user, then tries to pair. Returns match id if matched immediately.
     */
    public JoinResult join(long userId) {
        String uid = Long.toString(userId);
        redis.delete(assignmentKey(uid));
        redis.opsForList().remove(QUEUE_KEY, 0, uid);
        redis.opsForList().rightPush(QUEUE_KEY, uid);
        analyticsEventPublisher.publish("queue_join", userId, queueSize(), null);
        tryPairLocked();
        return buildJoinResult(userId);
    }

    /** Clears Redis assignment if it does not match game-service (stale id or wrong player). */
    public void invalidateAssignmentIfStale(long userId) {
        String uid = Long.toString(userId);
        String assigned = redis.opsForValue().get(assignmentKey(uid));
        if (assigned == null) {
            return;
        }
        try {
            int matchId = Integer.parseInt(assigned.trim());
            if (!gameServiceClient.matchContainsPlayer(matchId, userId)) {
                log.info("Removing stale matchmaking assignment userId={} matchId={}", userId, matchId);
                redis.delete(assignmentKey(uid));
            }
        } catch (NumberFormatException e) {
            redis.delete(assignmentKey(uid));
        }
    }

    public void leave(long userId) {
        String uid = Long.toString(userId);
        // If a match was already assigned, do not let a late "leave" clear queue/match flow.
        // This prevents racey UI polling from reporting "left queue" after assignment.
        String assigned = redis.opsForValue().get(assignmentKey(uid));
        if (assigned != null) {
            return;
        }
        redis.opsForList().remove(QUEUE_KEY, 0, uid);
        analyticsEventPublisher.publish("queue_leave", userId, queueSize(), null);
    }

    /**
     * Removes the FIFO queue and all {@code matchmaking:assigned:*} keys. Use when Redis still holds data from an
     * older matchmaking build or bad local testing (dev / local profile endpoint only).
     */
    public int purgeAllMatchmakingKeys() {
        Set<String> assignedKeys = redis.keys(ASSIGNED_PREFIX + "*");
        int n = assignedKeys == null ? 0 : assignedKeys.size();
        if (assignedKeys != null && !assignedKeys.isEmpty()) {
            redis.delete(assignedKeys);
        }
        redis.delete(QUEUE_KEY);
        log.warn("Purged matchmaking Redis: list {} and {} assignment keys", QUEUE_KEY, n);
        return n;
    }

    public StatusResult status(long userId) {
        String uid = Long.toString(userId);
        String assigned = redis.opsForValue().get(assignmentKey(uid));
        if (assigned != null) {
            try {
                int matchId = Integer.parseInt(assigned.trim());
                return new StatusResult(false, null, queueSize(), matchId);
            } catch (NumberFormatException e) {
                redis.delete(assignmentKey(uid));
            }
        }
        List<String> queue = Objects.requireNonNullElse(redis.opsForList().range(QUEUE_KEY, 0, -1), List.of());
        int idx = queue.indexOf(uid);
        if (idx >= 0) {
            return new StatusResult(true, idx + 1, queue.size(), null);
        }
        return new StatusResult(false, null, queueSize(), null);
    }

    private JoinResult buildJoinResult(long userId) {
        StatusResult s = status(userId);
        if (s.matchId() != null) {
            return new JoinResult("matched", userId, s.queueSize(), s.matchId());
        }
        return new JoinResult("queued", userId, s.queueSize(), null);
    }

    private int queueSize() {
        Long n = redis.opsForList().size(QUEUE_KEY);
        return n == null ? 0 : n.intValue();
    }

    /**
     * Pairs the first two players in the FIFO queue.
     * <p>
     * Important: we must NOT dequeue players until {@link GameServiceClient#createMatch} succeeds.
     * Otherwise, while game-service is creating the match, {@link #status} sees neither
     * {@code inQueue} nor {@code matchId}, and the front-end briefly thinks the user “left the queue”
     * exactly when a match was found.
     * </p>
     */
    private void tryPairLocked() {
        synchronized (pairLock) {
            while (true) {
                Long len = redis.opsForList().size(QUEUE_KEY);
                if (len == null || len < 2) {
                    return;
                }
                List<String> head = redis.opsForList().range(QUEUE_KEY, 0, 1);
                if (head == null || head.size() < 2) {
                    return;
                }
                String a = head.get(0);
                String b = head.get(1);
                long ida;
                long idb;
                try {
                    ida = Long.parseLong(a);
                    idb = Long.parseLong(b);
                } catch (NumberFormatException e) {
                    log.warn("Invalid queue entries at head, removing first: {}", a);
                    redis.opsForList().remove(QUEUE_KEY, 1, a);
                    continue;
                }
                if (ida == idb) {
                    redis.opsForList().remove(QUEUE_KEY, 1, a);
                    continue;
                }
                List<Long> players = new ArrayList<>(List.of(ida, idb));
                players.sort(Comparator.naturalOrder());
                int matchId;
                try {
                    matchId = gameServiceClient.createMatch(players);
                } catch (Exception e) {
                    log.error("Pairing failed (players still in queue at head): {} and {}", ida, idb, e);
                    return;
                }
                Long removedA = redis.opsForList().remove(QUEUE_KEY, 1, a);
                Long removedB = redis.opsForList().remove(QUEUE_KEY, 1, b);
                if (removedA == null || removedA == 0 || removedB == null || removedB == 0) {
                    log.error(
                            "Could not dequeue users {} and {} after match {} was created — assignments still written",
                            ida,
                            idb,
                            matchId);
                }
                String mid = Integer.toString(matchId);
                redis.opsForValue().set(assignmentKey(Long.toString(ida)), mid, ASSIGNMENT_TTL);
                redis.opsForValue().set(assignmentKey(Long.toString(idb)), mid, ASSIGNMENT_TTL);
                log.info("Paired users {} and {} into new match {}", ida, idb, matchId);
                analyticsEventPublisher.publish("match_created", ida, queueSize(), (long) matchId);
                analyticsEventPublisher.publish("match_created", idb, queueSize(), (long) matchId);
            }
        }
    }

    private static String assignmentKey(String userId) {
        return ASSIGNED_PREFIX + userId;
    }

    public record JoinResult(String status, long userId, int queueSize, Integer matchId) {}

    public record StatusResult(boolean inQueue, Integer position, int queueSize, Integer matchId) {}
}
