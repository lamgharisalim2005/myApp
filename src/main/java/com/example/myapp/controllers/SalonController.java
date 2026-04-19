package com.example.myapp.controllers;

import com.example.myapp.dtos.*;
import com.example.myapp.exceptions.GlobalResponse;
import com.example.myapp.services.CoiffeurService;
import com.example.myapp.services.ClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/salons")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SalonController {

    private final CoiffeurService coiffeurService;
    private final ClientService clientService;

    // PUBLIC — Voir tous les salons
    @GetMapping
    public ResponseEntity<GlobalResponse<List<SalonResponse>>> getAllSalons() {
        List<SalonResponse> salons = clientService.getAllSalons();
        return ResponseEntity.ok(new GlobalResponse<>(salons));
    }

    // PUBLIC — Voir les détails d'un salon
    @GetMapping("/{salonId}/detail")
    public ResponseEntity<GlobalResponse<SalonDetailResponse>> getSalonDetail(
            @PathVariable UUID salonId) {
        SalonDetailResponse salon = clientService.getSalonDetail(salonId);
        return ResponseEntity.ok(new GlobalResponse<>(salon));
    }

    // PUBLIC — Voir les photos d'un salon
    @GetMapping("/{salonId}/photos")
    public ResponseEntity<GlobalResponse<List<PhotoResponse>>> getPhotosSalon(
            @PathVariable UUID salonId) {
        List<PhotoResponse> photos = coiffeurService.getPhotosSalon(salonId);
        return ResponseEntity.ok(new GlobalResponse<>(photos));
    }

    // COIFFEUR — Créer un salon → 201 Created
    @PostMapping
    public ResponseEntity<GlobalResponse<SalonResponse>> creerSalon(
            @RequestBody CreateSalonRequest request) {
        SalonResponse salon = coiffeurService.creerSalon(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new GlobalResponse<>(salon));
    }

    // ADMIN — Supprimer le salon → 204 No Content
    @DeleteMapping("/{salonId}")
    public ResponseEntity<Void> supprimerSalon(
            @PathVariable UUID salonId,
            @RequestParam UUID adminId) {
        coiffeurService.supprimerSalon(salonId, adminId);
        return ResponseEntity.noContent().build();
    }

    // ADMIN — Transférer la propriété
    @PutMapping("/{salonId}/transferer")
    public ResponseEntity<GlobalResponse<String>> transfererPropriete(
            @PathVariable UUID salonId,
            @RequestBody TransfererProprieteRequest request) {
        coiffeurService.transfererPropriete(salonId, request);
        return ResponseEntity.ok(new GlobalResponse<>("Propriété transférée avec succès"));
    }

    // ADMIN — Retirer un coiffeur du salon
    @PutMapping("/{salonId}/retirer")
    public ResponseEntity<GlobalResponse<String>> retirerCoiffeur(
            @PathVariable UUID salonId,
            @RequestBody RetirerCoiffeurRequest request) {
        coiffeurService.retirerCoiffeur(salonId, request);
        return ResponseEntity.ok(new GlobalResponse<>("Coiffeur retiré avec succès"));
    }

    // ADMIN — Ajouter une photo au salon → 201 Created
    @PostMapping("/{salonId}/photos")
    public ResponseEntity<GlobalResponse<PhotoResponse>> ajouterPhotoSalon(
            @PathVariable UUID salonId,
            @RequestParam UUID coiffeurId,
            @RequestParam("file") MultipartFile file) {
        PhotoResponse photo = coiffeurService.ajouterPhotoSalon(salonId, file, coiffeurId);
        return ResponseEntity.status(HttpStatus.CREATED).body(new GlobalResponse<>(photo));
    }

    // ADMIN — Supprimer une photo du salon → 204 No Content
    @DeleteMapping("/{salonId}/photos/{photoId}")
    public ResponseEntity<Void> supprimerPhotoSalon(
            @PathVariable UUID salonId,
            @PathVariable UUID photoId,
            @RequestParam UUID coiffeurId) {
        coiffeurService.supprimerPhotoSalon(photoId, coiffeurId);
        return ResponseEntity.noContent().build();
    }
}