package com.example.myapp.dtos;

import java.util.UUID;

public record PhotoResponse(
        UUID id,
        String url
) {
}