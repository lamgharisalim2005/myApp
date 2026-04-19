package com.example.myapp.dtos;

public record PaymentIntentResponse(
        String clientSecret,    // envoyé au frontend pour afficher le formulaire
        String paymentIntentId, // sauvegardé en DB pour vérifier le paiement après
        Double montant,
        String currency
) {
}
