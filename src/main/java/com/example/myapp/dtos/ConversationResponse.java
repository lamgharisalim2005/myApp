package com.example.myapp.dtos;

import java.time.LocalDateTime;
import java.util.UUID;

public record ConversationResponse(
        UUID userId,        // id de l'autre personne
        String userName,    // nom de l'autre personne
        String userType,    // "CLIENT" ou "COIFFEUR"
        String lastMessage, // dernier message
        LocalDateTime lastMessageTime // heure du dernier message
) {
}