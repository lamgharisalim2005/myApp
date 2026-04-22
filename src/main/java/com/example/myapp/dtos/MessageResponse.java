package com.example.myapp.dtos;

import java.time.LocalDateTime;
import java.util.UUID;

public record MessageResponse(
        UUID id,
        UUID senderId,      // ← userId de l'expéditeur
        String senderType,
        UUID receiverId,    // ← userId du destinataire
        String receiverType,
        String content,
        LocalDateTime createdAt,
        String status,
        boolean isMe
) {
}