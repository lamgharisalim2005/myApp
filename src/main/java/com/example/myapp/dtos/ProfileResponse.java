package com.example.myapp.dtos;

import java.util.UUID;

public record ProfileResponse(
        UUID userId,        // ← userId pour les deux client et coiffeur
        String name,
        String email,
        String profilePicture,
        String role,
        boolean isAdmin
) {
}
