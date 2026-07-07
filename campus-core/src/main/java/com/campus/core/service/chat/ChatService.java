package com.campus.core.service.chat;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.common.exception.BizException;
import com.campus.common.exception.ErrorCode;
import com.campus.core.model.entity.Announcement;
import com.campus.core.model.entity.Conversation;
import com.campus.core.model.entity.Message;
import com.campus.core.repository.mapper.ConversationMapper;
import com.campus.core.repository.mapper.MessageMapper;
import com.campus.core.service.search.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatClient.Builder chatClientBuilder;
    private final ConversationMapper conversationMapper;
    private final MessageMapper messageMapper;
    private final SearchService searchService;

    private static final String SYSTEM_PROMPT = """
            你是长春师范大学的智慧校园AI助手，名叫"园宝"。你的职责：
            1. 回答校园相关问题（设施位置、办事流程、院系介绍等），基于数据库中的校园知识库回答
            2. 回答简洁、友善，200字以内
            3. 不确定时主动告知，并建议拨打对应部门电话咨询
            4. 如果用户问的是社团/公告/报名等，主动引导到对应功能
            5. 学校地址：吉林省长春市长吉北线677号
            6. 你拥有查询工具，当用户询问设施位置、办事流程、院系设置时，必须使用查询工具获取准确信息，不要凭自己的知识回答
            """;

    @Transactional
    public Conversation createConversation(Long userId) {
        Conversation conv = new Conversation();
        conv.setUserId(userId);
        conv.setTitle("新的对话");
        conversationMapper.insert(conv);
        return conv;
    }

    /**
     * 加载会话并校验归属。
     * 会话不存在 -> NOT_FOUND；存在但不属于该用户 -> FORBIDDEN。
     * 返回已校验的 Conversation，供调用方复用。
     */
    private Conversation loadOwnedConversation(Long conversationId, Long userId) {
        Conversation conv = conversationMapper.selectById(conversationId);
        if (conv == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "会话不存在");
        }
        if (!conv.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权访问该会话");
        }
        return conv;
    }

    @Transactional
    public String chat(Long conversationId, String userMessage, Long userId) {
        // 0. 校验会话归属
        Conversation owned = loadOwnedConversation(conversationId, userId);

        // 1. 保存用户消息
        Message msg = new Message();
        msg.setConversationId(conversationId);
        msg.setRole("user");
        msg.setContent(userMessage);
        messageMapper.insert(msg);

        // 2. 构建 AI 调用（带 Function Calling）
        String reply = chatClientBuilder.build()
                .prompt()
                .system(SYSTEM_PROMPT)
                .functions("queryFacilityLocation", "queryProcedure", "queryDepartment")
                .user(userMessage)
                .call()
                .content();

        // 3. 保存助手回复
        Message replyMsg = new Message();
        replyMsg.setConversationId(conversationId);
        replyMsg.setRole("assistant");
        replyMsg.setContent(reply);
        messageMapper.insert(replyMsg);

        // 4. 如果是第一条消息，更新会话标题（复用已加载的会话，避免重复查询）
        if (messageMapper.findByConversationId(conversationId).size() <= 2) {
            if ("新的对话".equals(owned.getTitle())) {
                String title = userMessage.length() > 20 ? userMessage.substring(0, 20) + "..." : userMessage;
                owned.setTitle(title);
                conversationMapper.updateById(owned);
            }
        }

        return reply;
    }

    public Flux<String> chatStream(Long conversationId, String userMessage, Long userId) {
        // 0. 校验会话归属
        loadOwnedConversation(conversationId, userId);

        // 1. 保存用户消息
        Message msg = new Message();
        msg.setConversationId(conversationId);
        msg.setRole("user");
        msg.setContent(userMessage);
        messageMapper.insert(msg);

        // 2. 流式调用 AI（带 Function Calling，让AI可以查数据库知识库）
        return chatClientBuilder.build()
                .prompt()
                .system(SYSTEM_PROMPT)
                .functions("queryFacilityLocation", "queryProcedure", "queryDepartment")
                .user(userMessage)
                .stream()
                .content();
    }

    public List<Message> getMessages(Long conversationId, Long userId) {
        // 校验归属后再查询消息
        loadOwnedConversation(conversationId, userId);
        return messageMapper.findByConversationId(conversationId);
    }

    public List<Conversation> getConversations(Long userId) {
        LambdaQueryWrapper<Conversation> qw = new LambdaQueryWrapper<>();
        qw.eq(Conversation::getUserId, userId)
          .orderByDesc(Conversation::getUpdatedAt);
        return conversationMapper.selectList(qw);
    }

    /**
     * 带历史上下文的多轮对话
     */
    @Transactional
    public String chatWithHistory(Long conversationId, String userMessage, Long userId) {
        // 0. 校验会话归属
        loadOwnedConversation(conversationId, userId);

        Message msg = new Message();
        msg.setConversationId(conversationId);
        msg.setRole("user");
        msg.setContent(userMessage);
        messageMapper.insert(msg);

        // Build history context as text
        List<Message> history = messageMapper.findByConversationId(conversationId);
        int start = Math.max(0, history.size() - 11);
        List<Message> recentHistory = history.subList(start, history.size() - 1); // exclude current user msg

        StringBuilder historyContext = new StringBuilder();
        for (Message h : recentHistory) {
            historyContext.append(h.getRole().equals("user") ? "用户: " : "园宝: ")
                .append(h.getContent()).append("\n");
        }

        String promptWithHistory = historyContext.isEmpty()
            ? userMessage
            : "以下是之前的对话历史：\n" + historyContext + "\n当前用户问题: " + userMessage;

        String reply = chatClientBuilder.build()
                .prompt()
                .system(SYSTEM_PROMPT)
                .functions("queryFacilityLocation", "queryProcedure", "queryDepartment")
                .user(promptWithHistory)
                .call()
                .content();

        Message replyMsg = new Message();
        replyMsg.setConversationId(conversationId);
        replyMsg.setRole("assistant");
        replyMsg.setContent(reply);
        messageMapper.insert(replyMsg);

        return reply;
    }

    /**
     * RAG Q&A: 先检索相关文档，再基于检索结果生成回答
     */
    @Transactional
    public String chatWithRAG(Long conversationId, String userMessage, Long userId) {
        // 0. 校验会话归属
        loadOwnedConversation(conversationId, userId);

        List<Announcement> docs = searchService.search(userMessage);

        StringBuilder context = new StringBuilder();
        if (!docs.isEmpty()) {
            context.append("以下是与用户问题相关的园区资料：\n");
            for (int i = 0; i < Math.min(3, docs.size()); i++) {
                context.append(String.format("【资料%d】%s\n%s\n\n",
                    i + 1, docs.get(i).getTitle(), docs.get(i).getContent()));
            }
        }

        Message msg = new Message();
        msg.setConversationId(conversationId);
        msg.setRole("user");
        msg.setContent(userMessage);
        messageMapper.insert(msg);

        String reply = chatClientBuilder.build()
                .prompt()
                .system(SYSTEM_PROMPT + "\n\n你可以参考以下资料回答用户问题：\n" + context)
                .user(userMessage)
                .call()
                .content();

        Message replyMsg = new Message();
        replyMsg.setConversationId(conversationId);
        replyMsg.setRole("assistant");
        replyMsg.setContent(reply);
        messageMapper.insert(replyMsg);

        return reply;
    }

    @Transactional
    public void deleteConversation(Long conversationId, Long userId) {
        // 校验归属后再删除
        loadOwnedConversation(conversationId, userId);
        messageMapper.delete(new LambdaQueryWrapper<Message>()
                .eq(Message::getConversationId, conversationId));
        conversationMapper.deleteById(conversationId);
    }

    @Transactional
    public void deleteAllConversations(Long userId) {
        List<Conversation> convs = getConversations(userId);
        for (Conversation conv : convs) {
            messageMapper.delete(new LambdaQueryWrapper<Message>()
                    .eq(Message::getConversationId, conv.getId()));
        }
        conversationMapper.delete(new LambdaQueryWrapper<Conversation>()
                .eq(Conversation::getUserId, userId));
    }
}
