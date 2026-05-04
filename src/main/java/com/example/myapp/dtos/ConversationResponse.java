package com.example.myapp.dtos;

import java.time.LocalDateTime;
import java.util.UUID;

public record ConversationResponse(
        UUID userId,
        String name,
        String userType,
        String lastMessage,
        LocalDateTime lastMessageTime,
        String profilePicture,
        int unreadCount
) {
}