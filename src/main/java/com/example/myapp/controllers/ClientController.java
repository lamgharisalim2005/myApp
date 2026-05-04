package com.example.myapp.controllers;

import com.example.myapp.dtos.*;
import com.example.myapp.exceptions.GlobalResponse;
import com.example.myapp.services.ClientService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/clients")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ClientController {

    private final ClientService clientService;

    // CLIENT — Voir son profil
    // userId extrait automatiquement du JWT
    @GetMapping("/profile")
    public ResponseEntity<GlobalResponse<ProfileResponse>> getClientProfile(
            HttpServletRequest httpRequest) {
        UUID userId = (UUID) httpRequest.getAttribute("userId");
        ProfileResponse profile = clientService.getClientProfile(userId);
        return ResponseEntity.ok(new GlobalResponse<>(profile));
    }

    // CLIENT — Modifier son profil
    // userId extrait automatiquement du JWT
    @PutMapping("/profile")
    public ResponseEntity<GlobalResponse<ProfileResponse>> updateClientProfile(
            @RequestParam(required = false) String name,
            @RequestParam(value = "file", required = false) MultipartFile file,
            HttpServletRequest httpRequest) {
        UUID userId = (UUID) httpRequest.getAttribute("userId");
        UpdateClientRequest request = new UpdateClientRequest(name);
        ProfileResponse profile = clientService.updateClientProfile(userId, request, file);
        return ResponseEntity.ok(new GlobalResponse<>(profile));
    }

    @GetMapping("/{userId}/public")
    public ResponseEntity<GlobalResponse<ProfileResponse>> getClientPublicProfile(
            @PathVariable UUID userId) {
        ProfileResponse profile = clientService.getClientPublicProfile(userId);
        return ResponseEntity.ok(new GlobalResponse<>(profile));
    }
}