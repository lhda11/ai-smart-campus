package com.campus.core.service.chat;

import com.campus.core.CampusApplication;
import com.campus.common.exception.BizException;
import com.campus.core.model.entity.Conversation;
import com.campus.core.model.entity.Message;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = CampusApplication.class)
@ActiveProfiles("dev")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ChatServiceTest {

    @Autowired
    private ChatService chatService;

    private static Long conversationId;
    private static final Long TEST_USER_ID = 1L;

    @Test
    @Order(1)
    @DisplayName("创建会话")
    void shouldCreateConversation() {
        Conversation conv = chatService.createConversation(TEST_USER_ID);
        assertNotNull(conv);
        assertNotNull(conv.getId());
        assertEquals(TEST_USER_ID, conv.getUserId());
        assertEquals("新的对话", conv.getTitle());
        conversationId = conv.getId();
    }

    @Test
    @Order(2)
    @DisplayName("发送消息并获取AI回复")
    void shouldSendMessageAndGetReply() {
        String reply = chatService.chat(conversationId, "你好，图书馆在哪里？", TEST_USER_ID);
        assertNotNull(reply);
        assertFalse(reply.isBlank());
    }

    @Test
    @Order(3)
    @DisplayName("获取会话消息列表")
    void shouldGetMessages() {
        List<Message> messages = chatService.getMessages(conversationId, TEST_USER_ID);
        assertFalse(messages.isEmpty());
        assertEquals(2, messages.size());
    }

    @Test
    @Order(4)
    @DisplayName("获取用户会话列表")
    void shouldGetConversations() {
        List<Conversation> convs = chatService.getConversations(TEST_USER_ID);
        assertFalse(convs.isEmpty());
    }

    @Test
    @Order(5)
    @DisplayName("拒绝跨用户访问会话")
    void shouldRejectCrossUserAccess() {
        // conversationId 属于 TEST_USER_ID(1L)，用 2L 访问应被拒绝
        BizException ex = assertThrows(BizException.class,
                () -> chatService.getMessages(conversationId, 2L));
        assertEquals(403, ex.getCode());
        // 跨用户删除同样应被拒绝
        BizException delEx = assertThrows(BizException.class,
                () -> chatService.deleteConversation(conversationId, 2L));
        assertEquals(403, delEx.getCode());
        // 跨用户续聊同样应被拒绝
        assertThrows(BizException.class,
                () -> chatService.chat(conversationId, "注入测试", 2L));
    }

    @Test
    @Order(6)
    @DisplayName("不存在的会话返回404")
    void shouldReturnNotFoundForMissingConversation() {
        BizException ex = assertThrows(BizException.class,
                () -> chatService.getMessages(99999999L, TEST_USER_ID));
        assertEquals(404, ex.getCode());
    }

    @Test
    @Order(7)
    @DisplayName("删除会话")
    void shouldDeleteConversation() {
        chatService.deleteConversation(conversationId, TEST_USER_ID);
        // 删除后再读取应返回404
        BizException ex = assertThrows(BizException.class,
                () -> chatService.getMessages(conversationId, TEST_USER_ID));
        assertEquals(404, ex.getCode());
    }
}
