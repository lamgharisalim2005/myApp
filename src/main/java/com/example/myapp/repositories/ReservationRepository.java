package com.example.myapp.repositories;


import com.example.myapp.entitys.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

    // Toutes les réservations d'un client
    List<Reservation> findByClientId(UUID clientId);

    // Toutes les réservations d'un coiffeur
    List<Reservation> findByCoiffeurId(UUID coiffeurId);

    // Vérifie si un coiffeur a déjà une réservation qui chevauche le créneau
    @Query("SELECT COUNT(r) > 0 FROM Reservation r WHERE r.coiffeur.id = :coiffeurId " +
            "AND r.status != 'CANCELLED' " +
            "AND r.startTime < :endTime AND r.endTime > :startTime")
    boolean existsConflict(
            @Param("coiffeurId") UUID coiffeurId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    // Toutes les réservations CONFIRMED dont endTime est dépassé
    @Query("SELECT r FROM Reservation r WHERE r.status = 'CONFIRMED' " +
            "AND r.endTime < :now")
    List<Reservation> findReservationsACompleter(@Param("now") LocalDateTime now);
    
}
