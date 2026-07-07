package com.campus.core.service.search;

import com.campus.core.model.entity.Announcement;
import com.campus.core.repository.mapper.AnnouncementMapper;
import com.campus.core.vector.VectorStoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {

    private final AnnouncementMapper announcementMapper;
    private final EmbeddingModel embeddingModel;
    private final VectorStoreService vectorStoreService;

    private static final int CHUNK_SIZE = 512;

    @Transactional
    public Announcement uploadDocument(String title, String category, MultipartFile file, Long authorId) {
        try {
            // Edge case: validate file
            if (file.isEmpty()) {
                throw new com.campus.common.exception.BizException(
                    com.campus.common.exception.ErrorCode.BAD_REQUEST, "文件为空");
            }
            String originalName = file.getOriginalFilename();
            if (originalName != null && originalName.contains(".")) {
                String ext = originalName.substring(originalName.lastIndexOf('.') + 1).toLowerCase();
                java.util.Set<String> allowed = java.util.Set.of("txt", "pdf", "docx");
                if (!allowed.contains(ext)) {
                    throw new com.campus.common.exception.BizException(
                        com.campus.common.exception.ErrorCode.BAD_REQUEST, "不支持的文件格式: " + ext);
                }
            }
            if (file.getSize() > 10 * 1024 * 1024) {
                throw new com.campus.common.exception.BizException(
                    com.campus.common.exception.ErrorCode.BAD_REQUEST, "文件大小超过10MB限制");
            }
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            Announcement ann = new Announcement();
            ann.setTitle(title);
            ann.setContent(content);
            ann.setCategory(category);
            ann.setAuthorId(authorId);
            ann.setStatus(1);
            announcementMapper.insert(ann);

            List<String> chunks = splitText(content, CHUNK_SIZE);
            float[] embedding = embed(chunks.get(0));
            String vectorId = vectorStoreService.storeVector(
                String.valueOf(ann.getId()), title, content, category, embedding);
            ann.setVectorId(vectorId);
            announcementMapper.updateById(ann);

            return ann;
        } catch (IOException e) {
            log.error("文件读取失败", e);
            throw new RuntimeException("文件上传失败", e);
        }
    }

    public List<Announcement> search(String query) {
        // Try vector search first
        try {
            float[] queryEmbedding = embed(query);
            List<String> docIds = vectorStoreService.searchSimilar(queryEmbedding, 5);
            if (!docIds.isEmpty()) {
                List<Long> ids = docIds.stream().map(Long::valueOf).collect(Collectors.toList());
                List<Announcement> results = announcementMapper.selectBatchIds(ids);
                if (!results.isEmpty()) return results;
            }
        } catch (Exception e) {
            log.warn("Vector search failed, falling back to text search: {}", e.getMessage());
        }
        // Fallback: MySQL LIKE search
        return announcementMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Announcement>()
                .like(Announcement::getTitle, query).or()
                .like(Announcement::getContent, query)
                .last("LIMIT 5")
        );
    }

    public List<Announcement> getByCategory(String category, int page, int size) {
        return announcementMapper.findByCategory(category);
    }

    public Announcement getById(Long id) {
        return announcementMapper.selectById(id);
    }

    @Transactional
    public void deleteAnnouncement(Long id) {
        vectorStoreService.deleteVector(String.valueOf(id));
        announcementMapper.deleteById(id);
    }

    private float[] embed(String text) {
        return embeddingModel.embed(text);
    }

    private List<String> splitText(String text, int chunkSize) {
        List<String> chunks = new ArrayList<>();
        for (int i = 0; i < text.length(); i += chunkSize) {
            int end = Math.min(i + chunkSize, text.length());
            chunks.add(text.substring(i, end));
        }
        return chunks;
    }
}
