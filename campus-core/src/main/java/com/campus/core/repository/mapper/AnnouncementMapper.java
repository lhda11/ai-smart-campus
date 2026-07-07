package com.campus.core.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.core.model.entity.Announcement;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AnnouncementMapper extends BaseMapper<Announcement> {

    @Select("SELECT * FROM t_announcement WHERE status = 1 AND category = #{category} ORDER BY created_at DESC")
    List<Announcement> findByCategory(String category);
}
