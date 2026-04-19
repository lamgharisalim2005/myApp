package com.example.myapp.dtos;

import java.util.UUID;

// Pour transférer la propriété du salon
public record TransfererProprieteRequest(
        UUID adminId,       // l'admin actuel
        UUID nouveauAdminId // le coiffeur qui devient admin
) {
}