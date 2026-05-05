package com.example.myapp.controllers;

import com.example.myapp.dtos.CreateReservationRequest;
import com.example.myapp.dtos.ReservationResponse;
import com.example.myapp.dtos.SlotResponse;
import com.example.myapp.exceptions.GlobalResponse;
import com.example.myapp.services.ReservationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ReservationController {

    private final ReservationService reservationService;

    // CLIENT — Créer une réservation → 201 Created
    // userId extrait automatiquement du JWT
    @PostMapping
    public ResponseEntity<GlobalResponse<ReservationResponse>> creerReservation(
            @RequestBody CreateReservationRequest request,
            HttpServletRequest httpRequest) {
        UUID userId = (UUID) httpRequest.getAttribute("userId");
        ReservationResponse response = reservationService.creerReservation(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(new GlobalResponse<>(response));
    }

    // COIFFEUR — Confirmer ou refuser une réservation
    // userId extrait automatiquement du JWT
    @PutMapping("/{reservationId}")
    public ResponseEntity<GlobalResponse<ReservationResponse>> traiterReservation(
            @PathVariable UUID reservationId,
            @RequestParam String decision,
            HttpServletRequest httpRequest) {
        UUID userId = (UUID) httpRequest.getAttribute("userId");
        ReservationResponse response = reservationService.traiterReservation(reservationId, decision, userId);
        return ResponseEntity.ok(new GlobalResponse<>(response));
    }

    // CLIENT — Voir ses réservations
    // userId extrait automatiquement du JWT
    @GetMapping("/client")
    public ResponseEntity<GlobalResponse<List<ReservationResponse>>> getReservationsClient(
            HttpServletRequest httpRequest) {
        UUID userId = (UUID) httpRequest.getAttribute("userId");
        List<ReservationResponse> reservations = reservationService.getReservationsClient(userId);
        return ResponseEntity.ok(new GlobalResponse<>(reservations));
    }

    // COIFFEUR — Voir ses réservations
    // userId extrait automatiquement du JWT
    @GetMapping("/coiffeur")
    public ResponseEntity<GlobalResponse<List<ReservationResponse>>> getReservationsCoiffeur(
            HttpServletRequest httpRequest) {
        UUID userId = (UUID) httpRequest.getAttribute("userId");
        List<ReservationResponse> reservations = reservationService.getReservationsCoiffeur(userId);
        return ResponseEntity.ok(new GlobalResponse<>(reservations));
    }

    // CLIENT — Voir les créneaux CONFIRMÉS futurs d'un coiffeur
    @GetMapping("/coiffeur/{coiffeurId}/slots")
    public ResponseEntity<GlobalResponse<List<SlotResponse>>> getConfirmedSlots(
            @PathVariable UUID coiffeurId,
            HttpServletRequest httpRequest) {
        UUID userId = (UUID) httpRequest.getAttribute("userId");
        List<SlotResponse> slots = reservationService.getConfirmedSlots(coiffeurId, userId);
        return ResponseEntity.ok(new GlobalResponse<>(slots));
    }

    // CLIENT — Annuler une réservation PENDING
    @PutMapping("/{reservationId}/cancel")
    public ResponseEntity<GlobalResponse<ReservationResponse>> annulerReservation(
            @PathVariable UUID reservationId,
            HttpServletRequest httpRequest) {
        UUID userId = (UUID) httpRequest.getAttribute("userId");
        ReservationResponse response = reservationService.annulerReservation(reservationId, userId);
        return ResponseEntity.ok(new GlobalResponse<>(response));
    }

    // CLIENT ET COIFFEUR — Supprimer une réservation passée
    @DeleteMapping("/{reservationId}")
    public ResponseEntity<GlobalResponse<String>> supprimerReservation(
            @PathVariable UUID reservationId,
            HttpServletRequest httpRequest) {
        UUID userId = (UUID) httpRequest.getAttribute("userId");
        reservationService.supprimerReservation(reservationId, userId);
        return ResponseEntity.ok(new GlobalResponse<>("Réservation supprimée"));
    }
}