package com.example.myapp.entitys;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Table(name = "Message")
public class Message {
    @Id
    @GeneratedValue(generator = "UUID")
    @UuidGenerator
    private UUID id;

    @Column(name = "content", nullable = false)
    private String content;

    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private UUID senderId;

    @Column(nullable = false)
    private String senderType;

    @Column(nullable = false)
    private UUID receiverId;

    @Column(nullable = false)
    private String status = "SENT"; // "SENT", "DELIVERED", "READ"

    @Column(nullable = false)
    private String receiverType;

    // Suppression côté sender (null = pas supprimé)
    @Column
    private LocalDateTime deletedBySenderAt;

    // Suppression côté receiver (null = pas supprimé)
    @Column
    private LocalDateTime deletedByReceiverAt;
}