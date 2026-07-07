package com.campus.core.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.core.model.entity.Knowledge;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface KnowledgeMapper extends BaseMapper<Knowledge> {

    @Select("SELECT * FROM t_knowledge WHERE status = 1 AND category = #{category} ORDER BY sort_order ASC")
    List<Knowledge> findByCategory(String category);
}
