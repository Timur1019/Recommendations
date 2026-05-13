package com.kurashnation.dto.response;

import java.time.Instant;

public record TrainingLibraryFileResponse(
        Long id,
        String title,
        String description,
        String originalFilename,
        String contentType,
        long sizeBytes,
        Instant createdAt,
        String viewUrl,
        String downloadUrl
) {
}

