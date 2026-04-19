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
@Table(name = "notification")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Notification {
    @Id
    @GeneratedValue(generator = "UUID")
    @UuidGenerator
    private UUID id;

    @Column(nullable = false)
    private String title;   // titre de la notification

    @Column(nullable = false)
    private String message; // contenu de la notification

    private boolean readStatus = false; // true = lu, false = non lu

    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String userType;

    @Column
    private UUID eventId;

    @Column
    private String eventType; // "RESERVATION", "SALON_REQUEST", "PAYMENT"


}
