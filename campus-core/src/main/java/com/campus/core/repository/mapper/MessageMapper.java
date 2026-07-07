package com.campus.core.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.core.model.entity.Message;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MessageMapper extends BaseMapper<Message> {

    @Select("SELECT * FROM t_message WHERE conversation_id = #{conversationId} ORDER BY created_at ASC")
    List<Message> findByConversationId(Long conversationId);
}
