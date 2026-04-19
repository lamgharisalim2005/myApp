package com.example.myapp.entitys;

import com.example.myapp.entitys.Service;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Table(name = "Reservation")
public class Reservation {
    @Id
    @GeneratedValue(generator = "UUID")
    @UuidGenerator
    private UUID id;

    @Column(nullable = false)
    private String status = "PENDING";//"PENDING" | "CONFIRMED" | "CANCELLED" | "COMPLETED"

    @Column(nullable = false)
    private LocalDateTime startTime; // début de la réservation

    @Column(nullable = false)
    private LocalDateTime endTime;   // calculé = startTime + service.duration

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clientId", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "coiffeurId", nullable = false)
    private Coiffeur coiffeur;

    // ✅ Dans Reservation — remplacez l'ancienne relation
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "reservation_service",
            joinColumns = @JoinColumn(name = "reservation_id"),
            inverseJoinColumns = @JoinColumn(name = "service_id")
    )
    private List<Service> services;


}
