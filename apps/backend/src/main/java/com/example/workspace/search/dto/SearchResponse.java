package com.example.workspace.search.dto;

import com.example.workspace.common.domain.SourceType;
import com.example.workspace.search.application.SearchText.Segment;
import com.example.workspace.tag.dto.TagDtos.Tag;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SearchResponse(List<Item> items,int page,int size,long total,int totalPages,Meta meta) {
    public record Item(SourceType type,UUID id,String title,Project project,List<Tag> tags,List<Segment> snippet,
                       List<String> matchedFields,boolean archived,Instant updatedAt) {}
    public record Project(UUID id,String name) {}
    public record Meta(boolean emptyQuery,boolean indexingPending,long tookMs) {}
}
