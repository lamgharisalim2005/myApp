package com.example.myapp.repositories;

import com.example.myapp.entitys.SalonRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SalonRequestRepository extends JpaRepository<SalonRequest, UUID> {
    List<SalonRequest> findBySalonId(UUID salonId);

    List<SalonRequest> findByCoiffeurId(UUID coiffeurId);

    boolean existsByCoiffeurIdAndSalonIdAndStatus(UUID coiffeurId, UUID salonId, String status);
}