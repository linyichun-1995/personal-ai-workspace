package com.example.workspace.file.dto;

import com.example.workspace.tag.dto.TagDtos.Tag;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class FileDtos {
    private FileDtos() {}
    public record CreateUpload(@NotBlank String name,@NotNull @PositiveOrZero Long size,String mediaType,UUID projectId) {}
    public record Upload(UUID uploadId,UUID fileId,String state,Instant expiresAt,String contentPath,String errorCode) {}
    public record CreatedUpload(Upload upload,boolean created) {}
    public record Update(@NotNull @PositiveOrZero Long version,String displayName,UUID projectId) {}
    public record Restore(@NotNull @PositiveOrZero Long version) {}
    public record Extraction(String status,boolean truncated,boolean retryable,String errorCode) {}
    public record Capabilities(boolean canEdit,boolean canDownload,boolean canRestore) {}
    public record Reference(String type,UUID id,String title) {}
    public record File(UUID id,String displayName,String originalName,String mediaType,long sizeBytes,UUID projectId,
                       String state,Extraction extraction,long version,long tagVersion,List<Tag> tags,List<Reference> references,
                       Capabilities capabilities,Instant createdAt,Instant updatedAt,Instant deletedAt,Instant purgeAfter,String deletionReason) {}
    public record Limits(Map<String,String> formats,long maxSize,int maxSelection,int maxConcurrent,long quotaBytes,long usedBytes,long reservedBytes,boolean storageAvailable) {}
    public record Text(String status,String text,boolean truncated) {}
}
