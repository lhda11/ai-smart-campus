package com.campus.core.controller;

import com.campus.common.dto.ApiResult;
import com.campus.core.model.entity.Announcement;
import com.campus.core.repository.mapper.AnnouncementMapper;
import com.campus.core.vector.VectorStoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "管理后台", description = "索引重建等管理操作")
public class AdminController {

    private final AnnouncementMapper announcementMapper;
    private final EmbeddingModel embeddingModel;
    private final VectorStoreService vectorStoreService;

    @PostMapping("/rebuild-index")
    @Operation(summary = "全量重建向量索引")
    public ApiResult<String> rebuildIndex() {
        List<Announcement> all = announcementMapper.selectList(null);
        int count = 0;
        for (Announcement ann : all) {
            if (ann.getContent() == null || ann.getContent().isBlank()) continue;
            String text = ann.getContent().length() > 512
                ? ann.getContent().substring(0, 512) : ann.getContent();
            float[] embedding = embeddingModel.embed(text);
            vectorStoreService.storeVector(
                String.valueOf(ann.getId()), ann.getTitle(),
                ann.getContent(), ann.getCategory(), embedding);
            ann.setVectorId(String.valueOf(ann.getId()));
            announcementMapper.updateById(ann);
            count++;
        }
        return ApiResult.ok("重建完成，处理 " + count + " 条文档");
    }
}
