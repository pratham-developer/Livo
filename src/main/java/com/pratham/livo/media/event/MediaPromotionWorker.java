package com.pratham.livo.media.event;

import com.pratham.livo.config.RabbitMQConfig;
import com.pratham.livo.entity.Hotel;
import com.pratham.livo.entity.Room;
import com.pratham.livo.media.port.StorageGateway;
import com.pratham.livo.repository.HotelRepository;
import com.pratham.livo.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaPromotionWorker {

    private final StorageGateway storageGateway;
    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;

    @RabbitListener(queues = RabbitMQConfig.MEDIA_QUEUE)
    public void handleMediaPromotion(MediaPromotionEvent event) {
        log.info("Worker picked up Media Promotion Event for {} ID: {}", event.entityType(), event.entityId());

        String userIdStr = String.valueOf(event.userId());
        String destinationPrefix = getDestinationPrefix(event);

        try {
            // 1. NETWORK CALLS (Not holding a DB connection)
            // Verify ownership of the temp files
            storageGateway.validateTemporaryFiles(userIdStr, event.newTempPaths());

            // Move the files to permanent storage
            List<String> promotedPaths = storageGateway.promoteToPermanent(event.newTempPaths(), destinationPrefix);

            // Combine the retained paths with the newly promoted paths
            List<String> finalPermanentPaths = new ArrayList<>(event.retainedPaths() != null ? event.retainedPaths() : List.of());
            finalPermanentPaths.addAll(promotedPaths);

            // 2. DATABASE COMMIT (Fast, short-lived transaction)
            updateEntityDatabase(event, finalPermanentPaths);

            // 3. CLEANUP (Network calls)
            cleanupOldFiles(event.newTempPaths(), event.pathsToDelete());

            log.info("Successfully completed Media Promotion for {} ID: {}", event.entityType(), event.entityId());

        } catch (Exception e) {
            log.error("Failed to process media promotion for {} ID: {}. Message will be retried or sent to DLQ.",
                    event.entityType(), event.entityId(), e);
            throw e; // Rethrowing tells RabbitMQ to retry or drop to DLQ
        }
    }

    @Transactional
    protected void updateEntityDatabase(MediaPromotionEvent event, List<String> finalPaths) {
        // This transaction is extremely fast - just updating a single column.
        if (event.entityType() == MediaPromotionEvent.EntityType.HOTEL) {
            Hotel hotel = hotelRepository.findById(event.entityId())
                    .orElseThrow(() -> new IllegalStateException("Hotel not found for media update"));
            hotel.setPhotos(finalPaths);
            hotelRepository.save(hotel);
        } else if (event.entityType() == MediaPromotionEvent.EntityType.ROOM) {
            Room room = roomRepository.findById(event.entityId())
                    .orElseThrow(() -> new IllegalStateException("Room not found for media update"));
            room.setPhotos(finalPaths);
            roomRepository.save(room);
        }
    }

    private void cleanupOldFiles(List<String> tempPaths, List<String> pathsToDelete) {
        // Delete the temp files we just promoted
        if (tempPaths != null && !tempPaths.isEmpty()) {
            try { storageGateway.deleteFiles(tempPaths); }
            catch (Exception e) { log.warn("Failed to delete temp files after promotion: {}", e.getMessage()); }
        }

        // Delete the old permanent files the user removed during an update
        if (pathsToDelete != null && !pathsToDelete.isEmpty()) {
            try { storageGateway.deleteFiles(pathsToDelete); }
            catch (Exception e) { log.warn("Failed to delete removed permanent files: {}", e.getMessage()); }
        }
    }

    private String getDestinationPrefix(MediaPromotionEvent event) {
        return switch (event.entityType()) {
            case HOTEL -> "hotels/" + event.entityId() + "/";
            case ROOM -> "rooms/" + event.entityId() + "/";
        };
    }
}