package com.kurashnation.dto.response;

public record AchievementMediaResponse(
        Long id,
        Long achievementId,
        String mediaKind,
        String originalFilename,
        String contentType,
        long sizeBytes,
        /** Относительный путь для GET с Authorization (не подходит для тега img src без blob). */
        String fileUrl
) {
}
