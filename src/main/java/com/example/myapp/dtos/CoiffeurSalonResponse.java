package com.example.myapp.dtos;

import java.util.UUID;

public record CoiffeurSalonResponse(
        UUID coiffeurId,    // ← nom coiffeurId mais valeur = userId
        String name,
        String profilePicture,
        boolean isAdmin
) {
}