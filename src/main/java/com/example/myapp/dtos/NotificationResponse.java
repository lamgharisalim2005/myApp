package com.example.myapp.dtos;

import java.util.UUID;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        String title,
        String message,
        boolean readStatus,
        LocalDateTime createdAt,
        UUID userId,
        String userType,
        UUID eventId,
        String eventType
) {
}

