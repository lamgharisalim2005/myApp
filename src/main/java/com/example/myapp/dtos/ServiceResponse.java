package com.example.myapp.dtos;

import java.util.UUID;

// Pas de changement nécessaire
public record ServiceResponse(
        UUID id,
        String name,
        String description,
        Double price,
        Integer duration
) {
}
