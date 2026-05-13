package com.kurashnation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TimeActivityRequest(
        @NotBlank @Size(max = 10) String time,
        @NotBlank @Size(max = 500) String activity
) {
}
