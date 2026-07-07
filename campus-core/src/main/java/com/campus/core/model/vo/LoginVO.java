package com.campus.core.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 登录响应
 */
@Data
@AllArgsConstructor
public class LoginVO {
    private String token;
    private Long userId;
    private String studentId;
    private String nickname;
    private Integer role;
    private boolean firstLogin;
}
