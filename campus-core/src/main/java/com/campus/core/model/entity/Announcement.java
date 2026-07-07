package com.campus.core.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("t_announcement")
public class Announcement {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String content;
    private String summary;
    private String category;
    private String fileUrl;
    private String vectorId;
    @TableLogic
    private Integer status;
    private Long authorId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
