package com.example.myapp.repositories;

import com.example.myapp.entitys.ReservationService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

public interface ReservationServiceRepository extends JpaRepository<ReservationService, UUID> {

    @Modifying
    @Transactional
    @Query("DELETE FROM ReservationService rs WHERE rs.service.id = :serviceId")
    void deleteByServiceId(UUID serviceId);
}