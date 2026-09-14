package com.example.workspace.infrastructure.redis;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RefreshTokenStore {

    private static final String KEY_PREFIX = "auth:refresh:";

    private final StringRedisTemplate redis;
    private final JsonMapper jsonMapper;

    public RefreshTokenStore(StringRedisTemplate redis, JsonMapper jsonMapper) {
        this.redis = redis;
        this.jsonMapper = jsonMapper;
    }

    public void save(String rawToken, RefreshSession session, Duration ttl) {
        redis.opsForValue().set(key(rawToken), write(session), ttl);
    }

    public Optional<RefreshSession> find(String rawToken) {
        String payload = redis.opsForValue().get(key(rawToken));
        if (payload == null) {
            return Optional.empty();
        }
        return Optional.of(read(payload));
    }

    public void delete(String rawToken) {
        redis.delete(key(rawToken));
    }

    private String key(String rawToken) {
        return KEY_PREFIX + sha256(rawToken);
    }

    private String write(RefreshSession session) {
        try {
            return jsonMapper.writeValueAsString(session);
        }
        catch (JacksonException exception) {
            throw new IllegalStateException("Unable to serialize refresh session", exception);
        }
    }

    private RefreshSession read(String payload) {
        try {
            return jsonMapper.readValue(payload, RefreshSession.class);
        }
        catch (JacksonException exception) {
            throw new IllegalStateException("Unable to deserialize refresh session", exception);
        }
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        }
        catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
