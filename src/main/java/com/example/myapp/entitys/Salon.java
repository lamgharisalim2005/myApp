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
@Table(name = "salon", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"latitude", "longitude"})
})
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Salon {
    @Id
    @GeneratedValue(generator = "UUID")
    @UuidGenerator
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String localisation; // adresse lisible ex: "Hay Riad, Rabat"

    @Column(nullable = false)
    private Double latitude;  // ex: 33.9716

    @Column(nullable = false)
    private Double longitude; // ex: -6.8498

    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "coiffeurId", unique = true, nullable = false)
    private Coiffeur coiffeur;
}
