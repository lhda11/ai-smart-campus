package com.campus.core.vector;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class VectorStoreService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String VECTOR_PREFIX = "vec:announcement:";

    public String storeVector(String docId, String title, String content, String category, float[] embedding) {
        String key = VECTOR_PREFIX + docId;

        ByteBuffer buffer = ByteBuffer.allocate(embedding.length * 4).order(ByteOrder.LITTLE_ENDIAN);
        for (float f : embedding) {
            buffer.putFloat(f);
        }
        byte[] vectorBytes = buffer.array();

        redisTemplate.execute((org.springframework.data.redis.connection.RedisConnection connection) -> {
            byte[] rawKey = redisTemplate.getStringSerializer().serialize(key);
            if (rawKey == null) return null;
            connection.hashCommands().hSet(rawKey,
                redisTemplate.getStringSerializer().serialize("vector"), vectorBytes);
            connection.hashCommands().hSet(rawKey,
                redisTemplate.getStringSerializer().serialize("title"),
                redisTemplate.getStringSerializer().serialize(title));
            connection.hashCommands().hSet(rawKey,
                redisTemplate.getStringSerializer().serialize("content"),
                redisTemplate.getStringSerializer().serialize(content));
            connection.hashCommands().hSet(rawKey,
                redisTemplate.getStringSerializer().serialize("category"),
                redisTemplate.getStringSerializer().serialize(category));
            connection.hashCommands().hSet(rawKey,
                redisTemplate.getStringSerializer().serialize("doc_id"),
                redisTemplate.getStringSerializer().serialize(docId));
            return null;
        });

        log.info("Vector stored: key={}", key);
        return docId;
    }

    public void deleteVector(String docId) {
        redisTemplate.delete(VECTOR_PREFIX + docId);
    }

    public List<String> searchSimilar(float[] queryEmbedding, int topK) {
        // FT.SEARCH KNN requires Lettuce native commands which have compatibility issues.
        // Primary search falls back to MySQL LIKE in SearchService.
        log.debug("Vector KNN search not available, use text fallback");
        return Collections.emptyList();
    }
}
