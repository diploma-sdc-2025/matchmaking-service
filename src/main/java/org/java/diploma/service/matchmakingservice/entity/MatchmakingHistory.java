package org.java.diploma.service.matchmakingservice.entity;

import jakarta.persistence.*;
        import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "matchmaking_history")
public class MatchmakingHistory {

    private static final String COLUMN_USER_ID = "user_id";
    private static final String COLUMN_JOINED_AT = "joined_at";
    private static final String COLUMN_MATCHED_AT = "matched_at";
    private static final String COLUMN_LEFT_QUEUE_AT = "left_queue_at";
    private static final String COLUMN_MATCH_ID = "match_id";
    private static final String COLUMN_WAIT_TIME_SECONDS = "wait_time_seconds";
    private static final String COLUMN_CANCEL_REASON = "cancel_reason";


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = COLUMN_USER_ID, nullable = false)
    private Long userId;

    @Column(name = COLUMN_JOINED_AT, nullable = false)
    private Instant joinedAt;

    @Column(name = COLUMN_MATCHED_AT)
    private Instant matchedAt;

    @Column(name = COLUMN_LEFT_QUEUE_AT)
    private Instant leftQueueAt;

    @Column(name = COLUMN_MATCH_ID)
    private Long matchId;

    @Column(name = COLUMN_WAIT_TIME_SECONDS)
    private Integer waitTimeSeconds;

    @Column(nullable = false)
    private String status;  // WAITING, MATCHED, CANCELLED

    @Column(name = COLUMN_CANCEL_REASON)
    private String cancelReason;
}