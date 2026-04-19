package com.example.myapp.repositories;

import com.example.myapp.entitys.Salon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SalonRepository extends JpaRepository<Salon, UUID> {
}
