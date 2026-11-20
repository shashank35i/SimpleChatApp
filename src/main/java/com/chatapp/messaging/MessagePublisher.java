package com.chatapp.messaging;

import com.chatapp.repository.RedisKeys;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class MessagePublisher {
    private final StringRedisTemplate redis;

    public MessagePublisher(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void publish(String roomId, String json) {
        redis.convertAndSend(RedisKeys.channel(roomId), json);
    }
}
