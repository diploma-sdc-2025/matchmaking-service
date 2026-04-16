package org.java.diploma.service.matchmakingservice.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.java.diploma.service.matchmakingservice.security.JwtUserIdFilter;
import org.java.diploma.service.matchmakingservice.service.MatchQueueService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/matchmaking")
public class MatchmakingController {

    private final MatchQueueService queueService;

    public MatchmakingController(MatchQueueService queueService) {
        this.queueService = queueService;
    }

    @PostMapping("/join")
    public ResponseEntity<Map<String, Object>> join(HttpServletRequest request) {
        long userId = requireUserId(request);
        queueService.invalidateAssignmentIfStale(userId);
        MatchQueueService.JoinResult r = queueService.join(userId);
        return ResponseEntity.ok(joinBody(r));
    }

    @PostMapping("/leave")
    public ResponseEntity<Void> leave(HttpServletRequest request) {
        long userId = requireUserId(request);
        queueService.leave(userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status(HttpServletRequest request) {
        long userId = requireUserId(request);
        MatchQueueService.StatusResult s = queueService.status(userId);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("inQueue", s.inQueue());
        body.put("position", s.position());
        body.put("queueSize", s.queueSize());
        if (s.matchId() != null) {
            body.put("matchId", s.matchId());
        }
        return ResponseEntity.ok(body);
    }

    private static Map<String, Object> joinBody(MatchQueueService.JoinResult r) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", r.status());
        body.put("userId", r.userId());
        body.put("queueSize", r.queueSize());
        if (r.matchId() != null) {
            body.put("matchId", r.matchId());
        }
        return body;
    }

    private static long requireUserId(HttpServletRequest request) {
        Object v = request.getAttribute(JwtUserIdFilter.ATTR_USER_ID);
        if (!(v instanceof Long userId)) {
            throw new IllegalStateException("userId not set by JWT filter");
        }
        return userId;
    }
}
