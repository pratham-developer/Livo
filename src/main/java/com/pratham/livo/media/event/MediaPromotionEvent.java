package com.pratham.livo.media.event;

import java.util.List;

public record MediaPromotionEvent(
        Long entityId,
        EntityType entityType, // Enum: HOTEL, ROOM
        Long userId, // The ID of the manager who uploaded this
        List<String> newTempPaths, // Paths starting with "temp/" that need promotion
        List<String> retainedPaths, // Existing permanent paths being kept
        List<String> pathsToDelete // Old paths that were removed during an update
) {
    public enum EntityType {
        HOTEL, ROOM
    }
}