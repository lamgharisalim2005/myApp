package com.example.myapp.repositories;

import com.example.myapp.entitys.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

    // Récupère toute la conversation entre deux personnes
    @Query("SELECT m FROM Message m WHERE " +
            "(m.senderId = :user1 AND m.receiverId = :user2) OR " +
            "(m.senderId = :user2 AND m.receiverId = :user1) " +
            "ORDER BY m.createdAt ASC")
    List<Message> findConversation(
            @Param("user1") UUID user1,
            @Param("user2") UUID user2
    );

    // Récupère tous les utilisateurs avec qui on a échangé des messages
    @Query("SELECT DISTINCT m.senderId FROM Message m WHERE m.receiverId = :userId " +
            "UNION " +
            "SELECT DISTINCT m.receiverId FROM Message m WHERE m.senderId = :userId")
    List<UUID> findConversationPartners(@Param("userId") UUID userId);

    // Récupère le dernier message entre deux personnes
    @Query("SELECT m FROM Message m WHERE " +
            "(m.senderId = :user1 AND m.receiverId = :user2) OR " +
            "(m.senderId = :user2 AND m.receiverId = :user1) " +
            "ORDER BY m.createdAt DESC LIMIT 1")
    Optional<Message> findLastMessage(
            @Param("user1") UUID user1,
            @Param("user2") UUID user2
    );
}