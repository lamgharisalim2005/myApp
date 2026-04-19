package com.example.myapp.dtos;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

public record CreateWorkScheduleRequest(
        UUID coiffeurId,
        String dayOfWeek,    // "MONDAY", "TUESDAY"...
        LocalTime startTime,
        LocalTime endTime
) {
}
