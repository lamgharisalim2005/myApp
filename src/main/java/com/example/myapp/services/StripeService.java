package com.example.myapp.services;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import org.springframework.stereotype.Service;

@Service
public class StripeService {

    public PaymentIntent creerPaymentIntent(Double montant, String currency) {
        try {
            long montantEnCentimes = (long) (montant * 100);

            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(montantEnCentimes)
                    .setCurrency(currency)
                    // ← ajoutez ces deux lignes
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .setAllowRedirects(PaymentIntentCreateParams.AutomaticPaymentMethods.AllowRedirects.NEVER)
                                    .build()
                    )
                    .build();

            return PaymentIntent.create(params);

        } catch (StripeException e) {
            throw new RuntimeException("Erreur lors de la création du paiement : " + e.getMessage());
        }
    }

    public PaymentIntent recupererPaymentIntent(String paymentIntentId) {
        try {
            // Récupère le statut du paiement depuis Stripe
            return PaymentIntent.retrieve(paymentIntentId);
        } catch (StripeException e) {
            throw new RuntimeException("Erreur lors de la récupération du paiement : " + e.getMessage());
        }
    }
}