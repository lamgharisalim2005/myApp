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
@Table(name = "client")
public class Client {
    @Id
    @GeneratedValue(generator = "UUID")
    @UuidGenerator
    private UUID id;

    @Column(name = "email" , unique = true , nullable = false)
    private String email;

    @Column(name = "name", nullable = false )
    private String name;

    @Column(name = "profilePicture")
    private String profilePicture;

    @Column(name = "role" , nullable = false)
    private String role = "CLIENT";
}
