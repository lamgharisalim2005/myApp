package com.example.myapp.dtos;

import java.util.UUID;

// Pas de changement nécessaire
public record PhotoResponse(
        UUID id,
        String url
) {
}