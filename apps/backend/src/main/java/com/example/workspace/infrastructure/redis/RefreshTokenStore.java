package com.example.workspace.infrastructure.redis;

import com.example.workspace.common.util.TokenHashes;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Repository
public class RefreshTokenStore {

    private static final String KEY_PREFIX = "auth:refresh:";
    private static final String FAMILY_PREFIX = "auth:refresh-family:";
    private static final String USER_PREFIX = "auth:refresh-user:";

    private final StringRedisTemplate redis;
    private final JsonMapper jsonMapper;

    public RefreshTokenStore(StringRedisTemplate redis, JsonMapper jsonMapper) {
        this.redis = redis;
        this.jsonMapper = jsonMapper;
    }

    public void save(String rawToken, RefreshSession session, Duration ttl) {
        String hash = TokenHashes.sha256(rawToken);
        redis.opsForValue().set(tokenKey(hash), write(session), ttl);
        String familyKey = familyKey(session.familyId());
        redis.opsForSet().add(familyKey, hash);
        redis.expire(familyKey, ttl);
        String userKey = userKey(session.userId());
        redis.opsForSet().add(userKey, session.familyId().toString());
        redis.expire(userKey, ttl);
    }

    public Optional<RefreshSession> find(String rawToken) {
        String payload = redis.opsForValue().get(tokenKey(TokenHashes.sha256(rawToken)));
        if (payload == null) {
            return Optional.empty();
        }
        return Optional.of(read(payload));
    }

    public void revoke(String rawToken) {
        find(rawToken).ifPresent(session -> overwrite(rawToken, session.revoke()));
    }

    public void revokeFamily(UUID familyId) {
        String familyKey = familyKey(familyId);
        Set<String> hashes = redis.opsForSet().members(familyKey);
        if (hashes != null) {
            for (String hash : hashes) {
                redis.delete(tokenKey(hash));
            }
        }
        redis.delete(familyKey);
    }

    public void revokeAllForUser(UUID userId) {
        String userKey = userKey(userId);
        Set<String> familyIds = redis.opsForSet().members(userKey);
        if (familyIds != null) {
            for (String familyId : familyIds) {
                revokeFamily(UUID.fromString(familyId));
            }
        }
        redis.delete(userKey);
    }

    public void expire(String rawToken) {
        find(rawToken).ifPresent(session -> overwrite(rawToken, session.expireAt(Instant.now().minusSeconds(60))));
    }

    private void overwrite(String rawToken, RefreshSession session) {
        String key = tokenKey(TokenHashes.sha256(rawToken));
        Long ttlSeconds = redis.getExpire(key);
        if (ttlSeconds == null || ttlSeconds == -2) {
            redis.delete(key);
            return;
        }
        if (ttlSeconds < 0) {
            redis.opsForValue().set(key, write(session));
            return;
        }
        redis.opsForValue().set(key, write(session), Duration.ofSeconds(ttlSeconds));
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

    private static String tokenKey(String hash) {
        return KEY_PREFIX + hash;
    }

    private static String familyKey(UUID familyId) {
        return FAMILY_PREFIX + familyId;
    }

    private static String userKey(UUID userId) {
        return USER_PREFIX + userId;
    }
}
