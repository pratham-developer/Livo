package com.pratham.livo.media.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaEventPublisher {

    // Use Spring's internal event bus instead of RabbitMQ directly
    private final ApplicationEventPublisher applicationEventPublisher;

    public void publishPromotionEvent(MediaPromotionEvent event) {
        log.info("Registering Media Promotion Event in Spring context for {} ID: {}", event.entityType(), event.entityId());

        // This does NOT send to RabbitMQ yet. It just holds the event in memory.
        applicationEventPublisher.publishEvent(event);
    }
}