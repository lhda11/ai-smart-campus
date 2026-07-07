package com.campus.core.controller;

import com.campus.common.dto.ApiResult;
import com.campus.core.auth.UserContext;
import com.campus.core.model.dto.ApplyRequest;
import com.campus.core.model.entity.Application;
import com.campus.core.service.club.ClubService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/apply")
@RequiredArgsConstructor
@Tag(name = "报名管理", description = "社团纳新报名")
public class ApplyController {

    private final ClubService clubService;

    @PostMapping
    @Operation(summary = "提交报名")
    public ApiResult<Application> apply(@Valid @RequestBody ApplyRequest req) {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) userId = 1L;
        return ApiResult.ok(clubService.apply(
            req.getRecruitmentId(), userId,
            req.getName(), req.getStudentId(), req.getPhone()));
    }

    @GetMapping("/my")
    @Operation(summary = "我的报名记录")
    public ApiResult<List<Application>> myApplications() {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) userId = 1L;
        return ApiResult.ok(clubService.getMyApplications(userId));
    }
}
