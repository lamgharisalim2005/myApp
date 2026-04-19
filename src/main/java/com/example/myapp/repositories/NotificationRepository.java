package com.example.myapp.repositories;

import com.example.myapp.entitys.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    // Récupère toutes les notifications d'un utilisateur par son id et son type
    List<Notification> findByUserIdAndUserType(UUID userId, String userType);
}