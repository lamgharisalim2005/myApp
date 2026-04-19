package com.example.myapp.dtos;

import java.util.UUID;

public record CoiffeurSalonResponse(
        UUID id,
        String name,
        String profilePicture,
        boolean isAdmin
) {
}