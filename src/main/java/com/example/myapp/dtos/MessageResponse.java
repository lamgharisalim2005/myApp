package com.example.myapp.dtos;

import java.time.LocalDateTime;
import java.util.UUID;

public record MessageResponse(
        UUID id,
        UUID senderId,
        String senderType,
        UUID receiverId,
        String receiverType,
        String content,
        LocalDateTime createdAt,
        String status, // ← ajoutez ça
        boolean isMe  // ← true si c'est le message de l'utilisateur connecté
) {
}