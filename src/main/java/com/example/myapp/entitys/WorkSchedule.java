package com.example.myapp.entitys;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Table(name = "WorkSchedule")
public class WorkSchedule {
    @Id
    @GeneratedValue(generator = "UUID")
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "coiffeurId", nullable = false)
    private Coiffeur coiffeur;

    @Column(nullable = false)
    private LocalTime startTime; // début du travail

    @Column(nullable = false)
    private LocalTime endTime; // fin du travail


    @Column(nullable = false)
    private String dayOfWeek; // "MONDAY", "TUESDAY", "WEDNESDAY"...


    // Getters & Setters
}
