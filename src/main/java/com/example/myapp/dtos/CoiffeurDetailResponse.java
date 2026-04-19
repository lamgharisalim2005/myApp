package com.example.myapp.dtos;

import java.util.List;
import java.util.UUID;

public record CoiffeurDetailResponse(
        UUID id,
        String name,
        String email,
        String profilePicture,
        List<String> photos,
        List<ServiceResponse> services,
        SalonResponse salon  // ← ajoutez ça (null si pas de salon)
) {
}