package com.example.myapp.dtos;

public record UpdateServiceRequest(
        String name,
        String description,
        Double price,
        Integer duration
) {
}