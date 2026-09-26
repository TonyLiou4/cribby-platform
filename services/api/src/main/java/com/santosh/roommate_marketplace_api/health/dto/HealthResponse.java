package com.santosh.roommate_marketplace_api.health.dto;

import java.time.LocalDateTime;

public record HealthResponse(
        String status,
        String message,
        LocalDateTime timestamp
) {
}
