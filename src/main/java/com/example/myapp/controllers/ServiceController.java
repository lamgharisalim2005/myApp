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
@RequestMapping("/api/services")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ServiceController {

    private final CoiffeurService coiffeurService;


    // COIFFEUR — Créer un service → 201 Created
    @PostMapping
    public ResponseEntity<GlobalResponse<ServiceResponse>> creerService(
            @RequestBody CreateServiceRequest request) {
        ServiceResponse service = coiffeurService.creerService(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new GlobalResponse<>(service));
    }

    // COIFFEUR — Modifier un service
    @PutMapping("/{id}")
    public ResponseEntity<GlobalResponse<ServiceResponse>> modifierService(
            @PathVariable UUID id,
            @RequestParam UUID coiffeurId,
            @RequestBody UpdateServiceRequest request) {
        ServiceResponse service = coiffeurService.modifierService(id, request, coiffeurId);
        return ResponseEntity.ok(new GlobalResponse<>(service));
    }

    // COIFFEUR — Supprimer un service → 204 No Content
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimerService(
            @PathVariable UUID id,
            @RequestParam UUID coiffeurId) {
        coiffeurService.supprimerService(id, coiffeurId);
        return ResponseEntity.noContent().build();
    }
}