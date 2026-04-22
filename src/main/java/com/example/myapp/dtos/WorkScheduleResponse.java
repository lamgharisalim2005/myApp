package com.example.myapp.dtos;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

public record WorkScheduleResponse(
        UUID id,
        String dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        UUID coiffeurId    // ← nom coiffeurId mais valeur = userId
) {
}
