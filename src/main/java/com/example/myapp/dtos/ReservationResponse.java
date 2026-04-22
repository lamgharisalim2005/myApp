package com.example.myapp.dtos;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ReservationResponse(
        UUID id,
        String status,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String clientName,
        String coiffeurName,
        List<String> serviceNames,
        Double totalPrice
) {
}