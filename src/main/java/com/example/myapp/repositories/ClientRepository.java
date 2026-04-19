package com.example.myapp.repositories;

import com.example.myapp.entitys.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClientRepository extends JpaRepository<Client, UUID> {
    // Trouver un client par son email
    Optional<Client> findByEmail(String email);
}