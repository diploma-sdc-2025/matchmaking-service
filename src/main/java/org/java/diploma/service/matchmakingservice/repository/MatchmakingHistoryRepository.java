package org.java.diploma.service.matchmakingservice.repository;

import org.java.diploma.service.matchmakingservice.entity.MatchmakingHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MatchmakingHistoryRepository
        extends JpaRepository<MatchmakingHistory, Long> {

    Optional<MatchmakingHistory> findFirstByUserIdAndStatus(
            Long userId,
            String status
    );

    /** Latest row for this user in MATCHED state (for exposing game matchId after pairing). */
    Optional<MatchmakingHistory> findFirstByUserIdAndStatusOrderByMatchedAtDesc(
            Long userId,
            String status
    );
}
