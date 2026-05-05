package com.example.myapp.repositories;

import com.example.myapp.entitys.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

    // Récupère la conversation — filtre les messages supprimés par user1
    @Query("SELECT m FROM Message m WHERE " +
            "((m.senderId = :user1 AND m.receiverId = :user2) OR " +
            "(m.senderId = :user2 AND m.receiverId = :user1)) " +
            "AND (" +
            "  (m.senderId = :user1 AND (m.deletedBySenderAt IS NULL OR m.createdAt > m.deletedBySenderAt)) OR " +
            "  (m.receiverId = :user1 AND (m.deletedByReceiverAt IS NULL OR m.createdAt > m.deletedByReceiverAt))" +
            ") " +
            "ORDER BY m.createdAt ASC")
    List<Message> findConversation(
            @Param("user1") UUID user1,
            @Param("user2") UUID user2
    );

    // Récupère tous les partenaires de conversation
    @Query("SELECT DISTINCT m.senderId FROM Message m WHERE m.receiverId = :userId " +
            "UNION " +
            "SELECT DISTINCT m.receiverId FROM Message m WHERE m.senderId = :userId")
    List<UUID> findConversationPartners(@Param("userId") UUID userId);

    // Dernier message visible par user1
    @Query("SELECT m FROM Message m WHERE " +
            "((m.senderId = :user1 AND m.receiverId = :user2) OR " +
            "(m.senderId = :user2 AND m.receiverId = :user1)) " +
            "AND (" +
            "  (m.senderId = :user1 AND (m.deletedBySenderAt IS NULL OR m.createdAt > m.deletedBySenderAt)) OR " +
            "  (m.receiverId = :user1 AND (m.deletedByReceiverAt IS NULL OR m.createdAt > m.deletedByReceiverAt))" +
            ") " +
            "ORDER BY m.createdAt DESC LIMIT 1")
    Optional<Message> findLastMessage(
            @Param("user1") UUID user1,
            @Param("user2") UUID user2
    );

    // Compte les messages non lus non supprimés
    @Query("SELECT COUNT(m) FROM Message m WHERE " +
            "m.senderId = :partnerId AND m.receiverId = :userId " +
            "AND m.status != 'READ' " +
            "AND (m.deletedByReceiverAt IS NULL OR m.createdAt > m.deletedByReceiverAt)")
    int countUnreadMessages(
            @Param("userId") UUID userId,
            @Param("partnerId") UUID partnerId
    );

    // Supprimer la conversation côté sender
    @Modifying
    @Transactional
    @Query("UPDATE Message m SET m.deletedBySenderAt = :deletedAt WHERE " +
            "m.senderId = :userId AND m.receiverId = :partnerId " +
            "AND (m.deletedBySenderAt IS NULL OR m.createdAt > m.deletedBySenderAt)")
    void deleteConversationAsSender(
            @Param("userId") UUID userId,
            @Param("partnerId") UUID partnerId,
            @Param("deletedAt") LocalDateTime deletedAt
    );

    // Supprimer la conversation côté receiver
    @Modifying
    @Transactional
    @Query("UPDATE Message m SET m.deletedByReceiverAt = :deletedAt WHERE " +
            "m.receiverId = :userId AND m.senderId = :partnerId " +
            "AND (m.deletedByReceiverAt IS NULL OR m.createdAt > m.deletedByReceiverAt)")
    void deleteConversationAsReceiver(
            @Param("userId") UUID userId,
            @Param("partnerId") UUID partnerId,
            @Param("deletedAt") LocalDateTime deletedAt
    );

    // Supprimer un message individuel côté sender
    @Modifying
    @Transactional
    @Query("UPDATE Message m SET m.deletedBySenderAt = :deletedAt WHERE " +
            "m.id = :messageId AND m.senderId = :userId " +
            "AND m.deletedBySenderAt IS NULL")
    void deleteMessageAsSender(
            @Param("messageId") UUID messageId,
            @Param("userId") UUID userId,
            @Param("deletedAt") LocalDateTime deletedAt
    );

    // Supprimer un message individuel côté receiver
    @Modifying
    @Transactional
    @Query("UPDATE Message m SET m.deletedByReceiverAt = :deletedAt WHERE " +
            "m.id = :messageId AND m.receiverId = :userId " +
            "AND m.deletedByReceiverAt IS NULL")
    void deleteMessageAsReceiver(
            @Param("messageId") UUID messageId,
            @Param("userId") UUID userId,
            @Param("deletedAt") LocalDateTime deletedAt
    );
}