package com.example.myapp.dtos;

import java.util.UUID;

public record SalonRequestResponse(
        UUID id,
        String status,
        String coiffeurName,
        String salonName
) {
}
