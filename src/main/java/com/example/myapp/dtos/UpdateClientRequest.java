package com.example.myapp.dtos;

// On supprime profilePicture String et on utilise MultipartFile
public record UpdateClientRequest(
        String name
) {
}