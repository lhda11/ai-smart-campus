package com.campus.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    SUCCESS(200, "success"),
    BAD_REQUEST(400, "参数错误"),
    UNAUTHORIZED(401, "未登录"),
    FORBIDDEN(403, "无权限"),
    NOT_FOUND(404, "资源不存在"),
    CONFLICT(409, "资源冲突"),
    INTERNAL_ERROR(500, "服务器内部错误"),
    AI_SERVICE_ERROR(5001, "AI服务异常"),
    VECTOR_STORE_ERROR(5002, "向量存储异常"),
    FILE_UPLOAD_ERROR(5003, "文件上传失败"),
    DUPLICATE_APPLY(5004, "重复报名");

    private final int code;
    private final String message;
}
