package com.example.myapp.dtos;

import java.util.UUID;

public record CreateSalonRequest(
        String name,
        String localisation,
        Double latitude,
        Double longitude,
        UUID coiffeurId
) {
}