package com.example.myapp.dtos;

import java.util.List;
import java.util.UUID;

public record SalonDetailResponse(
        UUID id,
        String name,
        String localisation,
        Double latitude,
        Double longitude,
        List<String> photos,
        List<CoiffeurSalonResponse> coiffeurs
) {
}
