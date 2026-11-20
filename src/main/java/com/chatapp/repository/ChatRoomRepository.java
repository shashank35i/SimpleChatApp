package com.chatapp.repository;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public class ChatRoomRepository {
    private final StringRedisTemplate redis;

    public ChatRoomRepository(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public boolean create(String roomId) {
        // HSETNX is the atomic uniqueness gate for concurrent requests.
        Boolean created = redis.opsForHash().putIfAbsent(RedisKeys.meta(roomId), "roomName", roomId);
        if (!Boolean.TRUE.equals(created)) {
            return false;
        }
        redis.opsForHash().put(RedisKeys.meta(roomId), "createdAt", Instant.now().toString());
        return true;
    }

    public boolean exists(String roomId) {
        return Boolean.TRUE.equals(redis.opsForHash().hasKey(RedisKeys.meta(roomId), "roomName"));
    }

    public boolean join(String roomId, String participant) {
        return Long.valueOf(1)
                .equals(redis.opsForSet().add(RedisKeys.participants(roomId), participant));
    }

    public boolean isMember(String roomId, String participant) {
        return Boolean.TRUE.equals(
                redis.opsForSet().isMember(RedisKeys.participants(roomId), participant));
    }

    public void append(String roomId, String json, int max) {
        redis.opsForList().rightPush(RedisKeys.messages(roomId), json);
        redis.opsForList().trim(RedisKeys.messages(roomId), -max, -1);
    }

    public List<String> history(String roomId, int limit) {
        List<String> values = redis.opsForList().range(RedisKeys.messages(roomId), -limit, -1);
        return values == null ? List.of() : values;
    }

    public void delete(String roomId) {
        redis.delete(
                List.of(
                        RedisKeys.meta(roomId),
                        RedisKeys.participants(roomId),
                        RedisKeys.messages(roomId)));
    }
}
