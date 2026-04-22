package com.example.myapp.controllers;

import com.example.myapp.dtos.*;
import com.example.myapp.exceptions.GlobalResponse;
import com.example.myapp.services.ClientService;
import com.example.myapp.services.CoiffeurService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/coiffeurs")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CoiffeurController {

    private final CoiffeurService coiffeurService;
    private final ClientService clientService;

    // PUBLIC — Voir les détails d'un coiffeur
    @GetMapping("/{coiffeurId}/detail")
    public ResponseEntity<GlobalResponse<CoiffeurDetailResponse>> getCoiffeurDetail(
            @PathVariable UUID coiffeurId) {
        CoiffeurDetailResponse coiffeur = clientService.getCoiffeurDetail(coiffeurId);
        return ResponseEntity.ok(new GlobalResponse<>(coiffeur));
    }

    // PUBLIC — Voir les photos d'un coiffeur
    @GetMapping("/{coiffeurId}/photos")
    public ResponseEntity<GlobalResponse<List<PhotoResponse>>> getPhotosCoiffeur(
            @PathVariable UUID coiffeurId) {
        List<PhotoResponse> photos = coiffeurService.getPhotosCoiffeur(coiffeurId);
        return ResponseEntity.ok(new GlobalResponse<>(photos));
    }

    // COIFFEUR — Voir son profil
    // userId extrait automatiquement du JWT
    @GetMapping("/profile")
    public ResponseEntity<GlobalResponse<ProfileResponse>> getCoiffeurProfile(
            HttpServletRequest httpRequest) {
        UUID userId = (UUID) httpRequest.getAttribute("userId"); // ← extrait du JWT
        ProfileResponse profile = coiffeurService.getCoiffeurProfile(userId);
        return ResponseEntity.ok(new GlobalResponse<>(profile));
    }

    // COIFFEUR — Modifier son profil
    // userId extrait automatiquement du JWT
    @PutMapping("/profile")
    public ResponseEntity<GlobalResponse<ProfileResponse>> updateCoiffeurProfile(
            @RequestParam(required = false) String name,
            @RequestParam(value = "file", required = false) MultipartFile file,
            HttpServletRequest httpRequest) {
        UUID userId = (UUID) httpRequest.getAttribute("userId"); // ← extrait du JWT
        UpdateCoiffeurRequest request = new UpdateCoiffeurRequest(name);
        ProfileResponse profile = coiffeurService.updateCoiffeurProfile(userId, request, file);
        return ResponseEntity.ok(new GlobalResponse<>(profile));
    }

    // COIFFEUR — Ajouter une photo → 201 Created
    // userId extrait automatiquement du JWT
    @PostMapping("/photos")
    public ResponseEntity<GlobalResponse<PhotoResponse>> ajouterPhotoCoiffeur(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest httpRequest) {
        UUID userId = (UUID) httpRequest.getAttribute("userId"); // ← extrait du JWT
        PhotoResponse photo = coiffeurService.ajouterPhotoCoiffeur(userId, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(new GlobalResponse<>(photo));
    }

    // COIFFEUR — Supprimer une photo → 204 No Content
    // userId extrait automatiquement du JWT
    @DeleteMapping("/photos/{photoId}")
    public ResponseEntity<Void> supprimerPhotoCoiffeur(
            @PathVariable UUID photoId,
            HttpServletRequest httpRequest) {
        UUID userId = (UUID) httpRequest.getAttribute("userId"); // ← extrait du JWT
        coiffeurService.supprimerPhotoCoiffeur(photoId, userId);
        return ResponseEntity.noContent().build();
    }

    // COIFFEUR — Quitter le salon
    // userId extrait automatiquement du JWT
    @PutMapping("/quitter-salon")
    public ResponseEntity<GlobalResponse<String>> quitterSalon(
            HttpServletRequest httpRequest) {
        UUID userId = (UUID) httpRequest.getAttribute("userId"); // ← extrait du JWT
        coiffeurService.quitterSalon(userId);
        return ResponseEntity.ok(new GlobalResponse<>("Vous avez quitté le salon avec succès"));
    }
}