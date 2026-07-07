package com.campus.core.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.core.model.entity.Application;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ApplicationMapper extends BaseMapper<Application> {

    @Select("SELECT * FROM t_application WHERE user_id = #{userId} ORDER BY created_at DESC")
    List<Application> findByUserId(Long userId);

    @Select("SELECT COUNT(*) FROM t_application WHERE user_id = #{userId} AND recruitment_id = #{recruitmentId}")
    int countByUserAndRecruitment(Long userId, Long recruitmentId);
}
