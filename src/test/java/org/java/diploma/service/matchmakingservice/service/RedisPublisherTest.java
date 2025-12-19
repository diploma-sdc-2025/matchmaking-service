package org.java.diploma.service.matchmakingservice.service;

import org.java.diploma.service.matchmakingservice.event.MatchmakingEvent;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
class RedisPublisherTest {

    @Test
    void publish_shouldSendEventToRedis() {
        RedisTemplate<String, Object> redisTemplate =
                Mockito.mock(RedisTemplate.class);

        RedisPublisher publisher =
                new RedisPublisher(redisTemplate);

        MatchmakingEvent event = new MatchmakingEvent();
        event.setType("player_join");

        publisher.publish(event);

        Mockito.verify(redisTemplate)
                .convertAndSend("analytics:events", event);
    }
}
