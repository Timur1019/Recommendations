package com.kurashnation.dto.response;

public record DeficitItemResponse(
        String parameter,
        double current,
        double target,
        double difference
) {
}

