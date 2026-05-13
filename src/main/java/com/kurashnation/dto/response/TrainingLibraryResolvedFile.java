package com.kurashnation.dto.response;

import java.nio.file.Path;

public record TrainingLibraryResolvedFile(Path path, String contentType, String originalFilename) {
}
