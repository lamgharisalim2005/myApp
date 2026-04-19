package com.example.myapp.dtos;

import java.util.UUID;

public record SalonResponse(
        UUID id,
        String name,
        String localisation,
        Double latitude,    // ← ajouté
        Double longitude    // ← ajouté
) {
}
