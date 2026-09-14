package com.example.workspace.user.repository;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.table;

import com.example.workspace.common.exception.ConflictException;
import com.example.workspace.user.domain.User;
import com.example.workspace.user.domain.UserStatus;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Table;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepository {

    private static final Table<Record> USERS = table("users");

    private final DSLContext dsl;

    public UserRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public void insert(User user) {
        dsl.insertInto(USERS)
                .set(field("id"), user.id())
                .set(field("email"), user.email())
                .set(field("email_normalized"), user.emailNormalized())
                .set(field("password_hash"), user.passwordHash())
                .set(field("display_name"), user.displayName())
                .set(field("avatar_url"), user.avatarUrl())
                .set(field("locale"), user.locale())
                .set(field("timezone"), user.timezone())
                .set(field("status"), user.status().name())
                .set(field("created_at"), toOffset(user.createdAt()))
                .set(field("updated_at"), toOffset(user.updatedAt()))
                .set(field("last_login_at"), user.lastLoginAt() == null ? null : toOffset(user.lastLoginAt()))
                .set(field("version"), user.version())
                .execute();
    }

    public Optional<User> findById(UUID id) {
        return dsl.selectFrom(USERS)
                .where(field("id", UUID.class).eq(id))
                .fetchOptional(this::toUser);
    }

    public Optional<User> findByNormalizedEmail(String emailNormalized) {
        return dsl.selectFrom(USERS)
                .where(field("email_normalized", String.class).eq(emailNormalized))
                .fetchOptional(this::toUser);
    }

    public boolean existsByNormalizedEmail(String emailNormalized) {
        return dsl.fetchExists(
                dsl.selectOne().from(USERS).where(field("email_normalized", String.class).eq(emailNormalized))
        );
    }

    public void updateLastLoginAt(UUID userId, Instant lastLoginAt) {
        dsl.update(USERS)
                .set(field("last_login_at"), toOffset(lastLoginAt))
                .set(field("updated_at"), toOffset(lastLoginAt))
                .where(field("id", UUID.class).eq(userId))
                .execute();
    }

    public User updateProfile(
            UUID userId,
            long version,
            String displayName,
            String avatarUrl,
            String locale,
            String timezone,
            Instant now
    ) {
        int updated = dsl.update(USERS)
                .set(field("display_name"), displayName)
                .set(field("avatar_url"), avatarUrl)
                .set(field("locale"), locale)
                .set(field("timezone"), timezone)
                .set(field("updated_at"), toOffset(now))
                .set(field("version"), version + 1)
                .where(field("id", UUID.class).eq(userId))
                .and(field("version", Long.class).eq(version))
                .execute();
        if (updated == 0) {
            throw ConflictException.versionMismatch();
        }
        return findById(userId).orElseThrow(() -> new IllegalStateException("User not found after update: " + userId));
    }

    public User updatePassword(UUID userId, String passwordHash, Instant now) {
        int updated = dsl.update(USERS)
                .set(field("password_hash"), passwordHash)
                .set(field("updated_at"), toOffset(now))
                .where(field("id", UUID.class).eq(userId))
                .execute();
        if (updated == 0) {
            throw new IllegalStateException("User not found: " + userId);
        }
        return findById(userId).orElseThrow(() -> new IllegalStateException("User not found after password update: " + userId));
    }

    private User toUser(Record record) {
        return new User(
                record.get("id", UUID.class),
                record.get("email", String.class),
                record.get("email_normalized", String.class),
                record.get("password_hash", String.class),
                record.get("display_name", String.class),
                record.get("avatar_url", String.class),
                record.get("locale", String.class),
                record.get("timezone", String.class),
                UserStatus.valueOf(record.get("status", String.class)),
                toInstant(record.get("created_at")),
                toInstant(record.get("updated_at")),
                toInstantOrNull(record.get("last_login_at")),
                record.get("version", Number.class).longValue()
        );
    }

    private static OffsetDateTime toOffset(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }

    private static Instant toInstantOrNull(Object value) {
        if (value == null) {
            return null;
        }
        return toInstant(value);
    }

    private static Instant toInstant(Object value) {
        if (value instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime.toInstant();
        }
        if (value instanceof Instant instant) {
            return instant;
        }
        throw new IllegalStateException("Unsupported timestamp type: " + value.getClass());
    }
}
