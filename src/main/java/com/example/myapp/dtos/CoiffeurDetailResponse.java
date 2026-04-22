package com.example.myapp.dtos;

import java.util.List;
import java.util.UUID;

public record CoiffeurDetailResponse(
        UUID coiffeurId,    // ← nom coiffeurId mais valeur = userId
        String name,
        String email,
        String profilePicture,
        List<String> photos,
        List<ServiceResponse> services,
        SalonResponse salon
) {
}