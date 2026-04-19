package com.example.myapp.entitys;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Table(name = "Coiffeur")
public class Coiffeur {
    @Id
    @GeneratedValue(generator = "UUID")
    @UuidGenerator
    private UUID id;

    private boolean isAdmin = false;

    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "profilePicture")
    private String profilePicture;

    @Column(name = "role", nullable = false)
    private String role = "COIFFEUR";

//    @Column(name = "rib", nullable = false)
//    private String rib; // ex: 011780000123456789012345

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "salon_id", nullable = true,
            foreignKey = @ForeignKey(name = "fk_coiffeur_salon"))
    private Salon salon;
}
