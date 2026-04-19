package com.example.myapp.controllers;

import com.example.myapp.dtos.*;
import com.example.myapp.exceptions.GlobalResponse;
import com.example.myapp.services.ClientService;
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
    @GetMapping("/{clientId}/profile")
    public ResponseEntity<GlobalResponse<ProfileResponse>> getClientProfile(
            @PathVariable UUID clientId) {
        ProfileResponse profile = clientService.getClientProfile(clientId);
        return ResponseEntity.ok(new GlobalResponse<>(profile));
    }

    // CLIENT — Modifier son profil
    @PutMapping("/{clientId}/profile")
    public ResponseEntity<GlobalResponse<ProfileResponse>> updateClientProfile(
            @PathVariable UUID clientId,
            @RequestParam(required = false) String name,
            @RequestParam(value = "file", required = false) MultipartFile file) {
        UpdateClientRequest request = new UpdateClientRequest(name);
        ProfileResponse profile = clientService.updateClientProfile(clientId, request, file);
        return ResponseEntity.ok(new GlobalResponse<>(profile));
    }
}