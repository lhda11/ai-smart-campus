package com.campus.core.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("t_knowledge")
public class Knowledge {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String category;
    private String name;
    private String content;
    private Integer sortOrder;
    @TableLogic
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
