package com.example.myapp.controllers;

import com.example.myapp.exceptions.GlobalResponse;
import com.example.myapp.services.GoogleOAuth2Service;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private final GoogleOAuth2Service googleOAuth2Service;

    // PUBLIC — Connexion/Inscription avec Google
    @PostMapping("/google")
    public ResponseEntity<GlobalResponse<Map<String, String>>> loginWithGoogle(
            @RequestBody Map<String, String> request) {

        String googleIdToken = request.get("idToken");
        String role = request.get("role");

        if (googleIdToken == null || role == null) {
            throw new RuntimeException("idToken et role sont obligatoires");
        }

        // Retourne JWT + userId + clientId/coiffeurId
        Map<String, String> response = googleOAuth2Service.authenticateWithGoogle(googleIdToken, role);

        return ResponseEntity.ok(new GlobalResponse<>(response));
    }
}