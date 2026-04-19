package com.example.myapp.dtos;

import java.util.UUID;

public record JoinSalonRequest(
        UUID coiffeurId,
        UUID salonId
) {
}