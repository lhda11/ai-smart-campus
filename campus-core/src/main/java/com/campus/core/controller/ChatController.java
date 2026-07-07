package com.campus.core.controller;

import com.campus.common.dto.ApiResult;
import com.campus.common.exception.BizException;
import com.campus.common.exception.ErrorCode;
import com.campus.core.auth.UserContext;
import com.campus.core.model.dto.ChatSendRequest;
import com.campus.core.model.entity.Conversation;
import com.campus.core.model.entity.Message;
import com.campus.core.model.vo.ChatReplyVO;
import com.campus.core.service.chat.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Tag(name = "智能咨询", description = "AI对话和会话管理")
public class ChatController {

    private final ChatService chatService;

    /**
     * 解析当前登录用户。TokenInterceptor 已在拦截器层拒绝未登录请求，
     * 此处对 userId==null 抛 UNAUTHORIZED 以防配置失误时静默落到用户 1。
     */
    private Long currentUserId() {
        Long uid = UserContext.getCurrentUserId();
        if (uid == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "未登录，请先登录");
        }
        return uid;
    }

    @PostMapping(value = "/send", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "发送消息（SSE流式）")
    public Flux<String> send(@Valid @RequestBody ChatSendRequest req) {
        Long userId = currentUserId();
        Long convId = req.getConversationId();
        if (convId == null) {
            convId = chatService.createConversation(userId).getId();
        }
        return chatService.chatStream(convId, req.getMessage(), userId);
    }

    @GetMapping("/conversations")
    @Operation(summary = "获取会话列表")
    public ApiResult<List<Conversation>> conversations() {
        Long userId = currentUserId();
        return ApiResult.ok(chatService.getConversations(userId));
    }

    @GetMapping("/messages")
    @Operation(summary = "获取消息历史")
    public ApiResult<List<Message>> messages(@RequestParam Long convId) {
        Long userId = currentUserId();
        return ApiResult.ok(chatService.getMessages(convId, userId));
    }

    @DeleteMapping("/conversations/{convId}")
    @Operation(summary = "删除会话")
    public ApiResult<Void> deleteConversation(@PathVariable Long convId) {
        Long userId = currentUserId();
        chatService.deleteConversation(convId, userId);
        return ApiResult.ok();
    }

    @DeleteMapping("/conversations")
    @Operation(summary = "一键清空所有对话")
    public ApiResult<Void> deleteAllConversations() {
        Long userId = currentUserId();
        chatService.deleteAllConversations(userId);
        return ApiResult.ok();
    }

    @PostMapping("/send-test")
    @Operation(summary = "纯对话测试（无Tools）")
    public ApiResult<ChatReplyVO> sendTest(@Valid @RequestBody ChatSendRequest req) {
        Long userId = currentUserId();
        Long convId = req.getConversationId();
        if (convId == null) {
            convId = chatService.createConversation(userId).getId();
        }
        String reply = chatService.chat(convId, req.getMessage(), userId);
        return ApiResult.ok(new ChatReplyVO(convId, null, reply, "assistant"));
    }

    @PostMapping("/send-sync")
    @Operation(summary = "发送消息（同步，含上下文+Tools）")
    public ApiResult<ChatReplyVO> sendSync(@Valid @RequestBody ChatSendRequest req) {
        Long userId = currentUserId();
        Long convId = req.getConversationId();
        if (convId == null) {
            convId = chatService.createConversation(userId).getId();
        }
        String reply = chatService.chatWithHistory(convId, req.getMessage(), userId);
        return ApiResult.ok(new ChatReplyVO(convId, null, reply, "assistant"));
    }

    @PostMapping("/send-rag")
    @Operation(summary = "RAG智能问答（检索增强）")
    public ApiResult<ChatReplyVO> sendRAG(@Valid @RequestBody ChatSendRequest req) {
        Long userId = currentUserId();
        Long convId = req.getConversationId();
        if (convId == null) {
            convId = chatService.createConversation(userId).getId();
        }
        String reply = chatService.chatWithRAG(convId, req.getMessage(), userId);
        return ApiResult.ok(new ChatReplyVO(convId, null, reply, "assistant"));
    }
}
