package com.example.myapp.dtos;

import java.time.LocalDateTime;
import java.time.LocalTime;

public record UpdateWorkScheduleRequest(
        String dayOfWeek,
        LocalTime startTime,
        LocalTime endTime
) {
}
