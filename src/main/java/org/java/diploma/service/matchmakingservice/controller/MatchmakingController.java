package org.java.diploma.service.matchmakingservice.controller;

import org.java.diploma.service.matchmakingservice.dto.QueueStatusResponse;
import org.java.diploma.service.matchmakingservice.service.MatchmakingService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/matchmaking")
public class MatchmakingController {

    private final MatchmakingService service;

    public MatchmakingController(MatchmakingService service) {
        this.service = service;
    }

    @PostMapping("/join")
    public void join() {
        service.joinQueue();
    }

    @PostMapping("/leave")
    public void leave() {
        service.leaveQueue();
    }

    @GetMapping("/status")
    public QueueStatusResponse status() {
        return service.getQueueStatus();
    }
}
