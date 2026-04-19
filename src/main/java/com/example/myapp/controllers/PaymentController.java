package com.example.myapp.controllers;

import com.example.myapp.dtos.PaymentIntentResponse;
import com.example.myapp.dtos.PaymentRequest;
import com.example.myapp.exceptions.GlobalResponse;
import com.example.myapp.services.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PaymentController {

    private final PaymentService paymentService;

    // CLIENT — Créer un PaymentIntent → 201 Created
    // TODO: clientId sera extrait du JWT après Spring Security
    @PostMapping("/intent")
    public ResponseEntity<GlobalResponse<PaymentIntentResponse>> creerPaymentIntent(
            @RequestBody PaymentRequest request) {
        PaymentIntentResponse response = paymentService.creerPaymentIntent(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new GlobalResponse<>(response));
    }

    // SYSTÈME — Confirmer le paiement
    // TODO: En production ce endpoint sera appelé automatiquement par Stripe via Webhook
    @PutMapping("/confirmer/{paymentIntentId}")
    public ResponseEntity<GlobalResponse<String>> confirmerPaiement(
            @PathVariable String paymentIntentId) {
        paymentService.confirmerPaiement(paymentIntentId);
        return ResponseEntity.ok(new GlobalResponse<>("Paiement traité avec succès"));
    }
}