package com.campus.core.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("t_club")
public class Club {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String description;
    private String tags;
    private String logoUrl;
    private Integer memberCount;
    @TableLogic
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
