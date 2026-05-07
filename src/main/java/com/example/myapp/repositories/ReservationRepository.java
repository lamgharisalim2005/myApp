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

    @Query("SELECT COUNT(r) > 0 FROM Reservation r WHERE r.coiffeur.id = :coiffeurId " +
            "AND r.status IN ('CONFIRMED', 'WAITING_PAYMENT') " +
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

    List<Reservation> findByCoiffeurIdAndStatusAndStartTimeAfter(UUID id, String confirmed, LocalDateTime now);

    @Query("SELECT COUNT(r) > 0 FROM Reservation r WHERE r.client.id = :clientId " +
            "AND r.status IN ('PENDING', 'CONFIRMED', 'WAITING_PAYMENT') " +
            "AND r.startTime < :endTime AND r.endTime > :startTime")
    boolean existsClientConflict(
            @Param("clientId") UUID clientId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    // Slots occupés pour tout le monde (CONFIRMED + WAITING_PAYMENT)
    List<Reservation> findByCoiffeurIdAndStatusInAndStartTimeAfter(
            UUID coiffeurId,
            List<String> statuses,
            LocalDateTime startTime
    );

    // Slots occupés uniquement pour ce client (PENDING + REJECTED)
    List<Reservation> findByCoiffeurIdAndClientIdAndStatusInAndStartTimeAfter(
            UUID coiffeurId,
            UUID clientId,
            List<String> statuses,
            LocalDateTime startTime
    );

    // Trouver toutes les réservations PENDING qui chevauchent un créneau
// sauf la réservation confirmée elle-même
    @Query("SELECT r FROM Reservation r WHERE " +
            "r.coiffeur.id = :coiffeurId " +
            "AND r.id != :excludeId " +
            "AND r.status = 'PENDING' " +
            "AND r.startTime < :endTime " +
            "AND r.endTime > :startTime")
    List<Reservation> findConflictingPendingReservations(
            @Param("coiffeurId") UUID coiffeurId,
            @Param("excludeId") UUID excludeId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );
}
