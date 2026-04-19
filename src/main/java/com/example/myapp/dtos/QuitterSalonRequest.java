package com.example.myapp.dtos;

import java.util.UUID;

// Pour quitter le salon
public record QuitterSalonRequest(
        UUID coiffeurId
) {
}