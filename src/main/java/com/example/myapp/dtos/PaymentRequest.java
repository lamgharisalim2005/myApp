package com.example.myapp.dtos;

import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentRequest(
        UUID reservationId,
        String currency
) {
}