package com.campus.core.mq;

import com.campus.core.config.RabbitMqConfig;
import com.campus.core.vector.VectorStoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class VectorSyncConsumer {

    private final EmbeddingModel embeddingModel;
    private final VectorStoreService vectorStoreService;

    @RabbitListener(queues = RabbitMqConfig.QUEUE_VECTOR_SYNC)
    public void handleSync(VectorSyncMessage msg) {
        log.info("Vector sync: action={}, annId={}", msg.getAction(), msg.getAnnouncementId());
        try {
            String text = (msg.getContent() != null)
                ? msg.getContent().substring(0, Math.min(512, msg.getContent().length()))
                : "";
            float[] embedding = embeddingModel.embed(text);
            vectorStoreService.storeVector(
                String.valueOf(msg.getAnnouncementId()),
                msg.getTitle(), msg.getContent(), msg.getCategory(), embedding);
        } catch (Exception e) {
            log.error("向量同步失败: annId={}", msg.getAnnouncementId(), e);
            throw e; // trigger RabbitMQ retry
        }
    }

    @RabbitListener(queues = RabbitMqConfig.QUEUE_VECTOR_DELETE)
    public void handleDelete(VectorSyncMessage msg) {
        log.info("Vector delete: annId={}", msg.getAnnouncementId());
        vectorStoreService.deleteVector(String.valueOf(msg.getAnnouncementId()));
    }
}
