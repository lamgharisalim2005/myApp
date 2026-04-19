package com.example.myapp.dtos;

import java.util.UUID;

// Pour faire sortir un coiffeur du salon
public record RetirerCoiffeurRequest(
        UUID adminId,
        UUID coiffeurId
) {
}