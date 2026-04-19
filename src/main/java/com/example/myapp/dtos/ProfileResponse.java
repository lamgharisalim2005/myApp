package com.example.myapp.dtos;

import java.util.UUID;

public record ProfileResponse(
        UUID id,
        String name,
        String email,
        String profilePicture,
        String role
) {
}
