package com.campus.core.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("t_recruitment")
public class Recruitment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long clubId;
    private String title;
    private String requirement;
    private Integer quota;
    private Integer enrolledCount;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
