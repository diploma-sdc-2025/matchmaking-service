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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    @Column(name = "matched_at")
    private Instant matchedAt;

    @Column(name = "left_queue_at")
    private Instant leftQueueAt;

    @Column(name = "match_id")
    private Long matchId;

    @Column(name = "wait_time_seconds")
    private Integer waitTimeSeconds;

    @Column(nullable = false)
    private String status; // WAITING, MATCHED, CANCELLED
}
