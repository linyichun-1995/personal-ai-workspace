package com.example.workspace.search.dto;

import com.example.workspace.common.domain.SourceType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SearchQuery(String q,List<SourceType> types,UUID projectId,List<UUID> tagIds,
                          TagMode tagMode,Instant updatedFrom,Instant updatedTo,Boolean includeArchived,
                          String sort,@Min(1) @Max(100) Integer page,@Min(1) @Max(100) Integer size) {
    public enum TagMode { ALL, ANY }
    public int pageNumber() { return page==null?1:page; }
    public int pageSize() { return size==null?20:size; }
}
