package com.example.myapp.configs;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker // Active le système de messagerie WebSocket avec STOMP
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // "/queue" est le préfixe des messages envoyés par le SERVEUR vers le CLIENT
        // Exemple : le serveur envoie une notification à /queue/notifications/{userId}
        // Le client écoute sur ce chemin pour recevoir ses notifications en temps réel
        registry.enableSimpleBroker("/queue");

        // "/app" est le préfixe des messages envoyés par le CLIENT vers le SERVEUR
        // Exemple : le client envoie un message vers /app/chat
        // Le serveur reçoit ce message et le traite
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // "/ws" est le point d'entrée WebSocket
        // Le client se connecte via : ws://localhost:8080/ws
        // setAllowedOriginPatterns("*") autorise toutes les origines (frontend, mobile...)
        // withSockJS() permet un fallback si WebSocket n'est pas supporté par le navigateur
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}