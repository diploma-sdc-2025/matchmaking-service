package org.java.diploma.service.matchmakingservice.controller;

import org.java.diploma.service.matchmakingservice.service.MatchmakingService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/test")
public class TestController {

    private final MatchmakingService matchmakingService;

    public TestController(MatchmakingService matchmakingService) {
        this.matchmakingService = matchmakingService;
    }

    @GetMapping("/join/{userId}")
    public String joinQueue(@PathVariable Long userId) {
        matchmakingService.playerJoinedQueue(userId);
        return "User " + userId + " joined queue";
    }
}
