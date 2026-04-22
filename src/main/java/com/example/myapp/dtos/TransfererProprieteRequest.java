package com.example.myapp.dtos;

import java.util.UUID;

// Pour transférer la propriété du salon
public record TransfererProprieteRequest(
        UUID nouveauAdminId    // ← frontend envoie userId mais on l'appelle nouveauAdminId
) {
}