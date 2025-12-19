package org.java.diploma.service.matchmakingservice.repository;

import org.java.diploma.service.matchmakingservice.entity.MatchmakingHistory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class MatchmakingHistoryRepositoryTest {

    @Autowired
    MatchmakingHistoryRepository repository;

    @Test
    void findFirstByUserIdAndStatus_shouldReturnWaitingHistory() {
        MatchmakingHistory history = new MatchmakingHistory();
        history.setUserId(42L);
        history.setJoinedAt(Instant.now());
        history.setStatus("WAITING");

        repository.save(history);

        Optional<MatchmakingHistory> result =
                repository.findFirstByUserIdAndStatus(42L, "WAITING");

        assertTrue(result.isPresent());
        assertEquals("WAITING", result.get().getStatus());
    }
}
