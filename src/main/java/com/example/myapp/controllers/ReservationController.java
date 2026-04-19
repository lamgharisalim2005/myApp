package com.example.myapp.controllers;

import com.example.myapp.dtos.CreateReservationRequest;
import com.example.myapp.dtos.ReservationResponse;
import com.example.myapp.exceptions.GlobalResponse;
import com.example.myapp.services.ReservationService;
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
    @PostMapping
    public ResponseEntity<GlobalResponse<ReservationResponse>> creerReservation(
            @RequestBody CreateReservationRequest request) {
        ReservationResponse response = reservationService.creerReservation(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new GlobalResponse<>(response));
    }

    // COIFFEUR — Confirmer ou refuser une réservation
    // TODO: coiffeurId sera extrait du JWT après Spring Security
    @PutMapping("/{reservationId}")
    public ResponseEntity<GlobalResponse<ReservationResponse>> traiterReservation(
            @PathVariable UUID reservationId,
            @RequestParam String decision,
            @RequestParam UUID coiffeurId) {
        ReservationResponse response = reservationService.traiterReservation(reservationId, decision, coiffeurId);
        return ResponseEntity.ok(new GlobalResponse<>(response));
    }

    // CLIENT — Voir ses réservations
    // TODO: clientId sera extrait du JWT après Spring Security
    @GetMapping("/client/{clientId}")
    public ResponseEntity<GlobalResponse<List<ReservationResponse>>> getReservationsClient(
            @PathVariable UUID clientId) {
        List<ReservationResponse> reservations = reservationService.getReservationsClient(clientId);
        return ResponseEntity.ok(new GlobalResponse<>(reservations));
    }

    // COIFFEUR — Voir ses réservations
    // TODO: coiffeurId sera extrait du JWT après Spring Security
    @GetMapping("/coiffeur/{coiffeurId}")
    public ResponseEntity<GlobalResponse<List<ReservationResponse>>> getReservationsCoiffeur(
            @PathVariable UUID coiffeurId) {
        List<ReservationResponse> reservations = reservationService.getReservationsCoiffeur(coiffeurId);
        return ResponseEntity.ok(new GlobalResponse<>(reservations));
    }
}