package com.campus.core.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.core.model.entity.Conversation;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ConversationMapper extends BaseMapper<Conversation> {
}
