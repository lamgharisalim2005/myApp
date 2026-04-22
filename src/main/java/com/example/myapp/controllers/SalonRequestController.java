package com.example.myapp.controllers;

import com.example.myapp.dtos.*;
import com.example.myapp.exceptions.GlobalResponse;
import com.example.myapp.services.CoiffeurService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/salon-requests")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SalonRequestController {

    private final CoiffeurService coiffeurService;

    // COIFFEUR — Envoyer une demande → 201 Created
    // userId extrait automatiquement du JWT
    @PostMapping
    public ResponseEntity<GlobalResponse<SalonRequestResponse>> envoyerDemande(
            @RequestBody JoinSalonRequest request,
            HttpServletRequest httpRequest) {
        UUID userId = (UUID) httpRequest.getAttribute("userId");
        SalonRequestResponse response = coiffeurService.envoyerDemande(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(new GlobalResponse<>(response));
    }

    // ADMIN — Voir les demandes reçues par son salon
    // userId extrait automatiquement du JWT
    @GetMapping("/salon/{salonId}")
    public ResponseEntity<GlobalResponse<List<SalonRequestResponse>>> getDemandesBySalon(
            @PathVariable UUID salonId,
            HttpServletRequest httpRequest) {
        UUID userId = (UUID) httpRequest.getAttribute("userId");
        List<SalonRequestResponse> demandes = coiffeurService.getDemandesBySalon(salonId, userId);
        return ResponseEntity.ok(new GlobalResponse<>(demandes));
    }

    // COIFFEUR — Voir ses demandes envoyées
    // userId extrait automatiquement du JWT
    @GetMapping("/coiffeur")
    public ResponseEntity<GlobalResponse<List<SalonRequestResponse>>> getDemandesByCoiffeur(
            HttpServletRequest httpRequest) {
        UUID userId = (UUID) httpRequest.getAttribute("userId");
        List<SalonRequestResponse> demandes = coiffeurService.getDemandesByCoiffeur(userId);
        return ResponseEntity.ok(new GlobalResponse<>(demandes));
    }

    // ADMIN — Accepter ou refuser une demande
    // userId extrait automatiquement du JWT
    @PutMapping("/{demandeId}")
    public ResponseEntity<GlobalResponse<SalonRequestResponse>> traiterDemande(
            @PathVariable UUID demandeId,
            @RequestParam String status,
            HttpServletRequest httpRequest) {
        UUID userId = (UUID) httpRequest.getAttribute("userId");
        SalonRequestResponse response = coiffeurService.traiterDemande(demandeId, status, userId);
        return ResponseEntity.ok(new GlobalResponse<>(response));
    }
}