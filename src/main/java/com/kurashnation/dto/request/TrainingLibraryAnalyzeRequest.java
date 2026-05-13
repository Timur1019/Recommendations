package com.kurashnation.dto.request;

import jakarta.validation.constraints.Size;

public record TrainingLibraryAnalyzeRequest(
        Long athleteId,
        @Size(max = 2000) String coachNotes
) {
}
