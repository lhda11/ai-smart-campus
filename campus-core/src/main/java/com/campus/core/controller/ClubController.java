package com.campus.core.controller;

import com.campus.common.dto.ApiResult;
import com.campus.core.model.entity.Club;
import com.campus.core.model.entity.Recruitment;
import com.campus.core.service.club.ClubService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/club")
@RequiredArgsConstructor
@Tag(name = "社团推荐", description = "社团管理和兴趣推荐")
public class ClubController {

    private final ClubService clubService;

    @GetMapping("/recommend")
    @Operation(summary = "兴趣推荐社团")
    public ApiResult<List<Club>> recommend(@RequestParam String tags) {
        List<String> tagList = List.of(tags.split(","));
        return ApiResult.ok(clubService.recommend(tagList));
    }

    @GetMapping
    @Operation(summary = "所有社团")
    public ApiResult<List<Club>> listAll() {
        return ApiResult.ok(clubService.listClubs());
    }

    @GetMapping("/recommend/ai")
    @Operation(summary = "兴趣推荐社团（LLM精排）")
    public ApiResult<List<Club>> recommendAI(@RequestParam String tags) {
        List<String> tagList = List.of(tags.split(","));
        return ApiResult.ok(clubService.recommendWithLLM(tagList));
    }

    @GetMapping("/{id}")
    @Operation(summary = "社团详情")
    public ApiResult<Club> detail(@PathVariable Long id) {
        return ApiResult.ok(clubService.getClubById(id));
    }

    @GetMapping("/{clubId}/recruitments")
    @Operation(summary = "社团纳新活动列表")
    public ApiResult<List<Recruitment>> recruitments(@PathVariable Long clubId) {
        return ApiResult.ok(clubService.getRecruitmentsByClub(clubId));
    }
}
