package com.example.myapp.dtos;

import java.util.UUID;
import java.time.LocalDateTime;

public record SalonRequestResponse(
        UUID id,
        String status,
        String coiffeurName,
        String salonName,
        LocalDateTime createdAt
) {
}
