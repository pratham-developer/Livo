package com.pratham.livo.media.event;

import com.pratham.livo.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class MediaEventRelay {

    private final RabbitTemplate rabbitTemplate;

    // This method ONLY fires if the database transaction commits successfully.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void relayToRabbitMQ(MediaPromotionEvent event) {
        log.info("DB Commit successful. Relaying event to RabbitMQ for {} ID: {}", event.entityType(), event.entityId());

        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.MAIN_EXCHANGE,
                    RabbitMQConfig.MEDIA_ROUTING_KEY,
                    event
            );
        } catch (Exception e) {
            // If RabbitMQ is down right after DB commit, we catch the error so it doesn't crash the thread.
            // The images will stay in temp/ and be deleted by the cron job. The manager will have to re-upload.
            log.error("Failed to relay message to RabbitMQ. S3 files will remain in temp/.", e);
        }
    }
}