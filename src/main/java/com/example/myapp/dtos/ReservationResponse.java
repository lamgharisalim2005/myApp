package com.example.myapp.dtos;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

// ✅ Après
public record ReservationResponse(
        UUID id,
        String status,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String clientName,
        String coiffeurName,
        List<String> serviceNames,  // ← liste de noms de services
        Double totalPrice           // ← prix total de tous les services
) {
}