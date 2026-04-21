package com.example.myapp.repositories;

import com.example.myapp.entitys.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    // Trouver un utilisateur par email (pour vérifier s'il existe)
    Optional<User> findByEmail(String email);

    // Trouver un utilisateur par googleId (après connexion Google)
    Optional<User> findByGoogleId(String googleId);
}