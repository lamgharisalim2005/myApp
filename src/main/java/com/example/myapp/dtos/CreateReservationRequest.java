package com.example.myapp.dtos;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

// ✅ Après
public record CreateReservationRequest(
        UUID coiffeurId,
        List<UUID> serviceIds, // ← liste de services
        LocalDateTime startTime
) {
}