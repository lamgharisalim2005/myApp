package com.example.myapp.dtos;

import java.time.LocalDateTime;

public record SlotResponse(
        LocalDateTime startTime,
        LocalDateTime endTime
) {
}
