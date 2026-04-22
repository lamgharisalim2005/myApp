package com.example.myapp.dtos;

import java.util.UUID;

// Pas de changement nécessaire
public record SalonResponse(
        UUID id,
        String name,
        String localisation,
        Double latitude,
        Double longitude
) {
}
