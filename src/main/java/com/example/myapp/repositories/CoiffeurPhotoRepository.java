package com.example.myapp.repositories;

import com.example.myapp.entitys.CoiffeurPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CoiffeurPhotoRepository extends JpaRepository<CoiffeurPhoto, UUID> {
    // Récupère toutes les photos d'un coiffeur
    List<CoiffeurPhoto> findByCoiffeurId(UUID coiffeurId);
}