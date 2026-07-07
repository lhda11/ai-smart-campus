package com.campus.core.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ApplyRequest {
    private Long recruitmentId;
    @NotBlank
    private String name;
    @NotBlank
    private String studentId;
    @NotBlank
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
}
