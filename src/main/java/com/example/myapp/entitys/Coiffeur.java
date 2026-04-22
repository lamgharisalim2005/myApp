package com.example.myapp.entitys;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "coiffeur", indexes = {
        // Index unique — un seul admin par salon
        @Index(name = "unique_admin_per_salon",
                columnList = "salon_id",
                unique = true)  // ← sera appliqué avec WHERE is_admin = true en SQL
})
public class Coiffeur {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true,
            foreignKey = @ForeignKey(
                    name = "fk_coiffeur_user",
                    options = "ON DELETE CASCADE"  // ← CASCADE
            ))
    private User user;

    // Spécifique au coiffeur — est-il admin de son salon ?
    @Column(nullable = false)
    private boolean isAdmin = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "salon_id", nullable = true,
            foreignKey = @ForeignKey(
                    name = "fk_coiffeur_salon",
                    options = "ON DELETE SET NULL"  // ← SET NULL
            ))
    private Salon salon;
}