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
@RequestMapping("/api/workschedules")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class WorkScheduleController {

    private final CoiffeurService coiffeurService;

    // PUBLIC — Voir les horaires d'un coiffeur
    @GetMapping("/coiffeur/{coiffeurId}")
    public ResponseEntity<GlobalResponse<List<WorkScheduleResponse>>> getWorkSchedules(
            @PathVariable UUID coiffeurId) {
        List<WorkScheduleResponse> workSchedules = coiffeurService.getWorkSchedules(coiffeurId);
        return ResponseEntity.ok(new GlobalResponse<>(workSchedules));
    }

    // COIFFEUR — Créer un horaire → 201 Created
    // userId extrait automatiquement du JWT
    @PostMapping
    public ResponseEntity<GlobalResponse<WorkScheduleResponse>> creerWorkSchedule(
            @RequestBody CreateWorkScheduleRequest request,
            HttpServletRequest httpRequest) {
        UUID userId = (UUID) httpRequest.getAttribute("userId"); // ← extrait du JWT
        WorkScheduleResponse workSchedule = coiffeurService.creerWorkSchedule(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(new GlobalResponse<>(workSchedule));
    }

    // COIFFEUR — Modifier un horaire
    // userId extrait automatiquement du JWT
    @PutMapping("/{id}")
    public ResponseEntity<GlobalResponse<WorkScheduleResponse>> modifierWorkSchedule(
            @PathVariable UUID id,
            @RequestBody UpdateWorkScheduleRequest request,
            HttpServletRequest httpRequest) {
        UUID userId = (UUID) httpRequest.getAttribute("userId"); // ← extrait du JWT
        WorkScheduleResponse workSchedule = coiffeurService.modifierWorkSchedule(id, request, userId);
        return ResponseEntity.ok(new GlobalResponse<>(workSchedule));
    }

    // COIFFEUR — Supprimer un horaire → 204 No Content
    // userId extrait automatiquement du JWT
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimerWorkSchedule(
            @PathVariable UUID id,
            HttpServletRequest httpRequest) {
        UUID userId = (UUID) httpRequest.getAttribute("userId"); // ← extrait du JWT
        coiffeurService.supprimerWorkSchedule(id, userId);
        return ResponseEntity.noContent().build();
    }
}