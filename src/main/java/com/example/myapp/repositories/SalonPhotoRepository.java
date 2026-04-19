package com.example.myapp.repositories;

import com.example.myapp.entitys.SalonPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SalonPhotoRepository extends JpaRepository<SalonPhoto, UUID> {
    // Récupère toutes les photos d'un salon
    List<SalonPhoto> findBySalonId(UUID salonId);
}