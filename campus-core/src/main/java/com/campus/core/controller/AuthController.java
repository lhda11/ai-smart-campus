package com.campus.core.controller;

import com.campus.common.dto.ApiResult;
import com.campus.core.model.dto.ChangePasswordRequest;
import com.campus.core.model.dto.LoginRequest;
import com.campus.core.model.entity.User;
import com.campus.core.model.vo.LoginVO;
import com.campus.core.service.auth.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "认证管理", description = "登录、修改密码等")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "学号/工号登录")
    public ApiResult<LoginVO> login(@Valid @RequestBody LoginRequest req) {
        return ApiResult.ok(authService.login(req));
    }

    @PostMapping("/change-password")
    @Operation(summary = "修改密码（首次登录强制修改）")
    public ApiResult<LoginVO> changePassword(@Valid @RequestBody ChangePasswordRequest req) {
        return ApiResult.ok(authService.changePassword(req));
    }

    @GetMapping("/me")
    @Operation(summary = "获取当前用户信息")
    public ApiResult<User> me() {
        return ApiResult.ok(authService.getCurrentUser());
    }
}
