package com.campus.core.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatReplyVO {
    private Long conversationId;
    private Long messageId;
    private String content;
    private String role;
}
