package com.kurashnation.dto.request;

import jakarta.validation.constraints.Size;

public record SubmitTrainingRequestNoteDto(
        @Size(max = 2000) String note
) {
}
