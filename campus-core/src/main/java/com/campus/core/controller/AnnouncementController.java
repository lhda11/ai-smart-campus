package com.campus.core.controller;

import com.campus.common.dto.ApiResult;
import com.campus.core.auth.UserContext;
import com.campus.core.model.entity.Announcement;
import com.campus.core.service.search.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/announcements")
@RequiredArgsConstructor
@Tag(name = "园区墙", description = "公告、文档管理")
public class AnnouncementController {

    private final SearchService searchService;

    @PostMapping("/upload")
    @Operation(summary = "上传文档")
    public ApiResult<Announcement> upload(
            @RequestParam String title,
            @RequestParam String category,
            @RequestParam("file") MultipartFile file) {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) userId = 1L;
        return ApiResult.ok(searchService.uploadDocument(title, category, file, userId));
    }

    @GetMapping
    @Operation(summary = "按分类查询公告")
    public ApiResult<List<Announcement>> list(
            @RequestParam(defaultValue = "notice") String category,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResult.ok(searchService.getByCategory(category, page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "公告详情")
    public ApiResult<Announcement> detail(@PathVariable Long id) {
        Announcement ann = searchService.getById(id);
        if (ann == null) {
            return ApiResult.fail(404, "公告不存在");
        }
        return ApiResult.ok(ann);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除公告")
    public ApiResult<Void> delete(@PathVariable Long id) {
        searchService.deleteAnnouncement(id);
        return ApiResult.ok();
    }
}
