package com.example.myapp.dtos;

import java.util.UUID;

public record SendMessageRequest(
        UUID senderId,
        String senderType,   // "CLIENT" ou "COIFFEUR"
        UUID receiverId,
        String receiverType, // "CLIENT" ou "COIFFEUR"
        String content
) {
}
