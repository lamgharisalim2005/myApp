package com.example.myapp.configs;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final SimpMessagingTemplate messagingTemplate;

    // Map pour stocker les utilisateurs connectés
    public static final Map<UUID, Boolean> onlineUsers = new ConcurrentHashMap<>();

    @EventListener
    public void handleWebSocketConnect(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        if (accessor.getSessionAttributes() != null) {
            UUID userId = (UUID) accessor.getSessionAttributes().get("userId");
            if (userId != null) {
                onlineUsers.put(userId, true);
                // Notifier tout le monde que cet utilisateur est en ligne
                Map<String, Object> payload = new HashMap<>();
                payload.put("userId", userId.toString());
                payload.put("online", true);
                messagingTemplate.convertAndSend("/queue/online/" + userId, (Object) payload);
            }
        }
    }

    @EventListener
    public void handleWebSocketDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        if (accessor.getSessionAttributes() != null) {
            UUID userId = (UUID) accessor.getSessionAttributes().get("userId");
            if (userId != null) {
                onlineUsers.remove(userId);
                // Notifier tout le monde que cet utilisateur est hors ligne
                Map<String, Object> payload = new HashMap<>();
                payload.put("userId", userId.toString());
                payload.put("online", false);
                messagingTemplate.convertAndSend("/queue/online/" + userId, (Object) payload);
            }
        }
    }
}