package com.campus.core.model.dto;

import lombok.Data;

/**
 * 登录请求
 */
@Data
public class LoginRequest {
    private String studentId;
    private String password;
}
