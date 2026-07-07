package com.campus.core.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("t_application")
public class Application {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long recruitmentId;
    private Long userId;
    private String name;
    private String studentId;
    private String phone;
    private Integer status;
    private String remark;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
