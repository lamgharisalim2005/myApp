package com.example.myapp.configs;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final SimpMessagingTemplate messagingTemplate;

    // Map pour stocker les utilisateurs connectés
    public static final Map<UUID, Boolean> onlineUsers = new ConcurrentHashMap<>();


    @EventListener
    public void handleWebSocketConnect(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        System.out.println("🔌 Subscribe - Session attributes: " + accessor.getSessionAttributes());

        if (accessor.getSessionAttributes() != null) {
            UUID userId = (UUID) accessor.getSessionAttributes().get("userId");
            System.out.println("🔌 Subscribe - UserId: " + userId);
            if (userId != null && !onlineUsers.containsKey(userId)) {
                onlineUsers.put(userId, true);
                Map<String, Object> payload = new HashMap<>();
                payload.put("userId", userId.toString());
                payload.put("online", true);
                messagingTemplate.convertAndSend("/topic/online", (Object) payload);
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
                messagingTemplate.convertAndSend("/topic/online", (Object) payload);
            }
        }
    }
}