package com.example.myapp.controllers;

import com.example.myapp.dtos.*;
import com.example.myapp.exceptions.GlobalResponse;
import com.example.myapp.services.CoiffeurService;
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
    @PostMapping
    public ResponseEntity<GlobalResponse<SalonRequestResponse>> envoyerDemande(
            @RequestBody JoinSalonRequest request) {
        SalonRequestResponse response = coiffeurService.envoyerDemande(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new GlobalResponse<>(response));
    }

    // ADMIN — Voir les demandes reçues par son salon
    @GetMapping("/salon/{salonId}")
    public ResponseEntity<GlobalResponse<List<SalonRequestResponse>>> getDemandesBySalon(
            @PathVariable UUID salonId,
            @RequestParam UUID adminId) { // ← ajoutez ça
        List<SalonRequestResponse> demandes = coiffeurService.getDemandesBySalon(salonId, adminId);
        return ResponseEntity.ok(new GlobalResponse<>(demandes));
    }

    // COIFFEUR — Voir ses demandes envoyées
    @GetMapping("/coiffeur/{coiffeurId}")
    public ResponseEntity<GlobalResponse<List<SalonRequestResponse>>> getDemandesByCoiffeur(
            @PathVariable UUID coiffeurId) {
        List<SalonRequestResponse> demandes = coiffeurService.getDemandesByCoiffeur(coiffeurId);
        return ResponseEntity.ok(new GlobalResponse<>(demandes));
    }

    // ADMIN — Accepter ou refuser une demande
    @PutMapping("/{demandeId}")
    public ResponseEntity<GlobalResponse<SalonRequestResponse>> traiterDemande(
            @PathVariable UUID demandeId,
            @RequestParam String status,
            @RequestParam UUID adminId) {
        SalonRequestResponse response = coiffeurService.traiterDemande(demandeId, status, adminId);
        return ResponseEntity.ok(new GlobalResponse<>(response));
    }
}