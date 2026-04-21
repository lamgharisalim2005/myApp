package com.example.myapp.entitys;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "users") // "user" est un mot réservé dans PostgreSQL — on utilise "users"
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String name;

    @Column
    private String profilePicture;

    @Column(nullable = false)
    private String role; // "CLIENT" ou "COIFFEUR"

    @Column(unique = true)
    private String googleId;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}