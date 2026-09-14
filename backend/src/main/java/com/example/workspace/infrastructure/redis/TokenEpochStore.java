package com.example.workspace.infrastructure.redis;

import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class TokenEpochStore {

    private static final String KEY_PREFIX = "auth:user:";
    private static final String KEY_SUFFIX = ":token-epoch";

    private final StringRedisTemplate redis;

    public TokenEpochStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public long current(UUID userId) {
        String value = redis.opsForValue().get(key(userId));
        if (value == null || value.isBlank()) {
            return 0L;
        }
        return Long.parseLong(value);
    }

    public long increment(UUID userId) {
        Long next = redis.opsForValue().increment(key(userId));
        return next == null ? 1L : next;
    }

    private static String key(UUID userId) {
        return KEY_PREFIX + userId + KEY_SUFFIX;
    }
}
