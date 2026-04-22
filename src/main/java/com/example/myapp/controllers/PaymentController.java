package com.example.myapp.controllers;

import com.example.myapp.dtos.PaymentIntentResponse;
import com.example.myapp.dtos.PaymentRequest;
import com.example.myapp.exceptions.GlobalResponse;
import com.example.myapp.services.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PaymentController {

    private final PaymentService paymentService;

    // CLIENT — Créer un PaymentIntent → 201 Created
    // userId extrait automatiquement du JWT
    @PostMapping("/intent")
    public ResponseEntity<GlobalResponse<PaymentIntentResponse>> creerPaymentIntent(
            @RequestBody PaymentRequest request,
            HttpServletRequest httpRequest) {
        UUID userId = (UUID) httpRequest.getAttribute("userId");
        PaymentIntentResponse response = paymentService.creerPaymentIntent(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(new GlobalResponse<>(response));
    }

    // SYSTÈME — Confirmer le paiement
    // En production ce endpoint sera appelé automatiquement par Stripe via Webhook
    @PutMapping("/confirmer/{paymentIntentId}")
    public ResponseEntity<GlobalResponse<String>> confirmerPaiement(
            @PathVariable String paymentIntentId) {
        paymentService.confirmerPaiement(paymentIntentId);
        return ResponseEntity.ok(new GlobalResponse<>("Paiement traité avec succès"));
    }
}