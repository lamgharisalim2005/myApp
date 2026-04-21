package com.example.myapp.entitys;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "coiffeur")
public class Coiffeur {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Relation OneToOne vers User
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // Spécifique au coiffeur — est-il admin de son salon ?
    @Column(nullable = false)
    private boolean isAdmin = false;

    // Relation ManyToOne vers Salon (nullable)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "salon_id", nullable = true,
            foreignKey = @ForeignKey(name = "fk_coiffeur_salon"))
    private Salon salon;
}