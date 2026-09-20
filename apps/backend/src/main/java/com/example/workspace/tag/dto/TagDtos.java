package com.example.workspace.tag.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class TagDtos {
    private TagDtos() {}
    public enum Color { GRAY, RED, ORANGE, YELLOW, GREEN, BLUE, PURPLE, PINK }
    public record Create(@NotBlank String name, Color color) {}
    public record Update(String name, Color color, @NotNull @PositiveOrZero Long version) {}
    public record Replace(@NotNull List<@NotNull UUID> tagIds, @NotNull @PositiveOrZero Long tagVersion) {}
    public record Tag(UUID id, String name, Color color, long version, long referenceCount, Instant createdAt, Instant updatedAt) {}
    public record Collection(List<Tag> tags, long tagVersion) {}
}
