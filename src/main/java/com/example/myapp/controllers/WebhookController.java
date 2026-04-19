package com.example.myapp.controllers;

import com.example.myapp.services.PaymentService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
// Pas de @CrossOrigin — cet endpoint est appelé par Stripe uniquement
public class WebhookController {

    private final PaymentService paymentService;

    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

    // STRIPE — Webhook appelé automatiquement après chaque paiement
    // TODO: En production Stripe enverra le Webhook directement sans Stripe CLI
    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        Event event;

        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            return ResponseEntity.badRequest().body("Signature invalide");
        }

        switch (event.getType()) {
            case "payment_intent.succeeded":
                PaymentIntent paymentIntent = (PaymentIntent) event
                        .getDataObjectDeserializer()
                        .getObject()
                        .orElseThrow();
                paymentService.confirmerPaiement(paymentIntent.getId());
                break;

            case "payment_intent.payment_failed":
                PaymentIntent failedIntent = (PaymentIntent) event
                        .getDataObjectDeserializer()
                        .getObject()
                        .orElseThrow();
                paymentService.confirmerPaiement(failedIntent.getId());
                break;

            default:
                break;
        }

        return ResponseEntity.ok("Webhook reçu");
    }
}