package com.example.myapp.dtos;

import java.util.UUID;

public record CreateServiceRequest(
        String name,
        String description,
        Double price,
        Integer duration
) {
}
