package org.java.diploma.service.matchmakingservice.controller;

import org.java.diploma.service.matchmakingservice.service.MatchQueueService;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Local development: wipe matchmaking Redis keys. Enable with {@code SPRING_PROFILES_ACTIVE=dev} or {@code local}.
 */
@Profile({"dev", "local"})
@RestController
@RequestMapping("/api/matchmaking/dev")
public class MatchmakingDevController {

    private final MatchQueueService queueService;

    public MatchmakingDevController(MatchQueueService queueService) {
        this.queueService = queueService;
    }

    @PostMapping("/purge-redis")
    public ResponseEntity<Map<String, Object>> purgeRedis() {
        int assignmentsRemoved = queueService.purgeAllMatchmakingKeys();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("assignmentsRemoved", assignmentsRemoved);
        body.put("queueCleared", true);
        body.put("hint", "Game-service keys (game:state:*, game:king:*, …) are separate; clear those in redis-cli if matches desync.");
        return ResponseEntity.ok(body);
    }
}
