package org.java.diploma.service.matchmakingservice.controller;

import org.java.diploma.service.matchmakingservice.dto.QueueJoinResponse;
import org.java.diploma.service.matchmakingservice.dto.QueueStatusResponse;
import org.java.diploma.service.matchmakingservice.service.MatchmakingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/matchmaking")
public class MatchmakingController {

    private static final Logger logger = LoggerFactory.getLogger(MatchmakingController.class);

    private static final String PLAYER_JOINED_QUEUE = "Player joined matchmaking queue";
    private static final String PLAYER_LEFT_QUEUE = "Player left matchmaking queue";
    private static final String QUEUE_STATUS_REQUESTED = "Queue status requested";

    private final MatchmakingService service;

    public MatchmakingController(MatchmakingService service) {
        this.service = service;
    }

    @PostMapping("/join")
    public ResponseEntity<QueueJoinResponse> joinQueue(Authentication authentication) {
        logger.info(PLAYER_JOINED_QUEUE);
        return ResponseEntity.ok(service.joinQueue(authentication));
    }

    @PostMapping("/leave")
    public ResponseEntity<Void> leave(Authentication authentication) {
        logger.info(PLAYER_LEFT_QUEUE);
        service.leaveQueue(authentication);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/status")
    public ResponseEntity<QueueStatusResponse> status(Authentication authentication) {
        logger.debug(QUEUE_STATUS_REQUESTED);
        return ResponseEntity.ok(service.getQueueStatus(authentication));
    }
}