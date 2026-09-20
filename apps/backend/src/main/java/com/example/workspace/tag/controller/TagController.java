package com.example.workspace.tag.controller;

import com.example.workspace.common.api.PageQuery;
import com.example.workspace.common.api.PageResponse;
import com.example.workspace.common.domain.SourceType;
import com.example.workspace.common.security.CurrentUser;
import com.example.workspace.infrastructure.openapi.AuthenticatedApi;
import com.example.workspace.tag.application.TagService;
import com.example.workspace.tag.dto.TagDtos.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@AuthenticatedApi
@Tag(name = "Tags", description = "工作空间统一标签及四类资源标签集合")
public class TagController {
    private final TagService tags;
    public TagController(TagService tags) { this.tags=tags; }
    @GetMapping("/tags")
    public PageResponse<com.example.workspace.tag.dto.TagDtos.Tag> list(CurrentUser user, @RequestParam(required=false) String q, @Valid @ParameterObject PageQuery page) { return tags.list(user,q,page); }
    @PostMapping("/tags") @ResponseStatus(HttpStatus.CREATED)
    public com.example.workspace.tag.dto.TagDtos.Tag create(CurrentUser user,@Valid @RequestBody Create body) { return tags.create(user,body); }
    @PatchMapping("/tags/{id}")
    public com.example.workspace.tag.dto.TagDtos.Tag update(CurrentUser user,@PathVariable UUID id,@Valid @RequestBody Update body) { return tags.update(user,id,body); }
    @DeleteMapping("/tags/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(CurrentUser user,@PathVariable UUID id,@RequestHeader("If-Match") String version) { tags.delete(user,id,parseVersion(version)); }
    @GetMapping("/{resource:projects|tasks|notes|files}/{id}/tags")
    public Collection get(CurrentUser user,@PathVariable String resource,@PathVariable UUID id) { return tags.get(user,SourceType.fromResource(resource),id); }
    @PutMapping("/{resource:projects|tasks|notes|files}/{id}/tags")
    public Collection replace(CurrentUser user,@PathVariable String resource,@PathVariable UUID id,@Valid @RequestBody Replace body) { return tags.replace(user,SourceType.fromResource(resource),id,body); }
    public static long parseVersion(String value) {
        if (value != null && value.matches("(?:[0-9]+|\"[0-9]+\")")) {
            try { return Long.parseLong(value.replace("\"","")); } catch (NumberFormatException ignored) {}
        }
        throw com.example.workspace.common.domain.SourceAccess.invalid("If-Match 必须为非负版本号");
    }
}
