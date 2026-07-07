package com.campus.core.controller;

import com.campus.common.dto.ApiResult;
import com.campus.core.model.dto.SearchRequest;
import com.campus.core.model.entity.Announcement;
import com.campus.core.service.search.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@Tag(name = "智能检索", description = "语义搜索公告和文档")
public class SearchController {

    private final SearchService searchService;

    @PostMapping
    @Operation(summary = "语义搜索")
    public ApiResult<List<Announcement>> search(@Valid @RequestBody SearchRequest req) {
        return ApiResult.ok(searchService.search(req.getKeyword()));
    }
}
