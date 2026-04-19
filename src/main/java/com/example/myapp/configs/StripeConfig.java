package com.example.myapp.configs;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StripeConfig {

    // Récupère la clé secrète depuis application.properties
    @Value("${stripe.api.key}")
    private String secretKey;

    // @PostConstruct s'exécute automatiquement au démarrage de l'application
    // Il initialise Stripe avec votre clé secrète
    @PostConstruct
    public void init() {
        Stripe.apiKey = secretKey;
    }
}