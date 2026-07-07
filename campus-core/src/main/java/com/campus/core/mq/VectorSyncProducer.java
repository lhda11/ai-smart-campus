package com.campus.core.mq;

import com.campus.core.config.RabbitMqConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VectorSyncProducer {

    private final RabbitTemplate rabbitTemplate;

    public void sendUpsert(VectorSyncMessage msg) {
        rabbitTemplate.convertAndSend(RabbitMqConfig.QUEUE_VECTOR_SYNC, msg);
    }

    public void sendDelete(Long announcementId) {
        VectorSyncMessage msg = new VectorSyncMessage();
        msg.setAction("DELETE");
        msg.setAnnouncementId(announcementId);
        rabbitTemplate.convertAndSend(RabbitMqConfig.QUEUE_VECTOR_DELETE, msg);
    }
}
