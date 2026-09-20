package com.example.workspace.search.controller;

import com.example.workspace.common.security.CurrentUser;
import com.example.workspace.infrastructure.openapi.AuthenticatedApi;
import com.example.workspace.search.application.SearchService;
import com.example.workspace.search.dto.SearchQuery;
import com.example.workspace.search.dto.SearchResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/search")
@AuthenticatedApi
@Tag(name="Search",description="四类对象的全局关键词搜索，标签及生命周期实时过滤")
public class SearchController {
    private final SearchService service;
    public SearchController(SearchService service) { this.service=service; }
    @GetMapping public SearchResponse search(CurrentUser user,@Valid @ParameterObject SearchQuery query) { return service.search(user,query); }
}
