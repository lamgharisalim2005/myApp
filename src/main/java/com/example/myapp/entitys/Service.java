package com.example.myapp.entitys;

import com.example.myapp.entitys.Coiffeur;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "service")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Service {

    @Id
    @GeneratedValue(generator = "UUID")
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "coiffeurId", nullable = false)
    private Coiffeur coiffeur; // Le coiffeur qui propose ce service

    @Column(nullable = false)
    private String name; // Nom du service (ex: "Coupe homme")

    private String description; // Description optionnelle

    @Column(nullable = false)
    private double price; // Prix du service

    @Column(nullable = false)
    private int duration; // Durée en minutes

    // ✅ Dans Service — ajoutez cette relation
    @ManyToMany(mappedBy = "services", fetch = FetchType.LAZY)
    private List<Reservation> reservations;

    // Getters & Setters
}
