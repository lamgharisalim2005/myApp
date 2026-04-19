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
    @PostMapping
    public ResponseEntity<GlobalResponse<WorkScheduleResponse>> creerWorkSchedule(
            @RequestBody CreateWorkScheduleRequest request) {
        WorkScheduleResponse workSchedule = coiffeurService.creerWorkSchedule(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new GlobalResponse<>(workSchedule));
    }

    // COIFFEUR — Modifier un horaire
    @PutMapping("/{id}")
    public ResponseEntity<GlobalResponse<WorkScheduleResponse>> modifierWorkSchedule(
            @PathVariable UUID id,
            @RequestParam UUID coiffeurId,
            @RequestBody UpdateWorkScheduleRequest request) {
        WorkScheduleResponse workSchedule = coiffeurService.modifierWorkSchedule(id, request, coiffeurId);
        return ResponseEntity.ok(new GlobalResponse<>(workSchedule));
    }

    // COIFFEUR — Supprimer un horaire → 204 No Content
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimerWorkSchedule(
            @PathVariable UUID id,
            @RequestParam UUID coiffeurId) {
        coiffeurService.supprimerWorkSchedule(id, coiffeurId);
        return ResponseEntity.noContent().build();
    }
}