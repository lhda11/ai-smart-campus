package com.campus.core.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.core.model.entity.Recruitment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface RecruitmentMapper extends BaseMapper<Recruitment> {

    @Select("SELECT * FROM t_recruitment WHERE status = 1 AND club_id = #{clubId} ORDER BY created_at DESC")
    List<Recruitment> findActiveByClubId(Long clubId);
}
