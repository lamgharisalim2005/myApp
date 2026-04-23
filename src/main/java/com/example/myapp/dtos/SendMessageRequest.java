package com.example.myapp.dtos;

import java.util.UUID;

public record SendMessageRequest(
        UUID receiverId,
        String content
) {
}
