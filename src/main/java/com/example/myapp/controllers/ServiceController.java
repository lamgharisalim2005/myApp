package com.example.myapp.controllers;

import com.example.myapp.dtos.*;
import com.example.myapp.exceptions.GlobalResponse;
import com.example.myapp.services.CoiffeurService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/services")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ServiceController {

    private final CoiffeurService coiffeurService;

    // COIFFEUR — Créer un service → 201 Created
    // TODO: userId extrait automatiquement du JWT
    @PostMapping
    public ResponseEntity<GlobalResponse<ServiceResponse>> creerService(
            @RequestBody CreateServiceRequest request,
            HttpServletRequest httpRequest) {
        UUID userId = (UUID) httpRequest.getAttribute("userId"); // ← extrait du JWT
        ServiceResponse service = coiffeurService.creerService(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(new GlobalResponse<>(service));
    }

    // COIFFEUR — Modifier un service
    // TODO: userId extrait automatiquement du JWT
    @PutMapping("/{id}")
    public ResponseEntity<GlobalResponse<ServiceResponse>> modifierService(
            @PathVariable UUID id,
            @RequestBody UpdateServiceRequest request,
            HttpServletRequest httpRequest) {
        UUID userId = (UUID) httpRequest.getAttribute("userId"); // ← extrait du JWT
        ServiceResponse service = coiffeurService.modifierService(id, request, userId);
        return ResponseEntity.ok(new GlobalResponse<>(service));
    }

    // COIFFEUR — Supprimer un service → 204 No Content
    // TODO: userId extrait automatiquement du JWT
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimerService(
            @PathVariable UUID id,
            HttpServletRequest httpRequest) {
        UUID userId = (UUID) httpRequest.getAttribute("userId"); // ← extrait du JWT
        coiffeurService.supprimerService(id, userId);
        return ResponseEntity.noContent().build();
    }
}