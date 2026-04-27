package com.example.myapp.services;

import com.example.myapp.dtos.NotificationResponse;
import com.example.myapp.entitys.Notification;
import com.example.myapp.entitys.User;
import com.example.myapp.repositories.ClientRepository;
import com.example.myapp.repositories.CoiffeurRepository;
import com.example.myapp.repositories.NotificationRepository;
import com.example.myapp.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public void envoyerNotification(UUID userId, String title,
                                    String message, UUID eventId, String eventType) {
        // 1. Sauvegarder en base de données
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setUserType(userRepository.findById(userId).get().getRole());
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setReadStatus(false);
        notification.setCreatedAt(LocalDateTime.now());
        notification.setEventId(eventId);
        notification.setEventType(eventType);
        Notification saved = notificationRepository.save(notification);
        // Envoyer via WebSocket en temps réel
        messagingTemplate.convertAndSend(
                "/queue/notifications/" + userId,
                new NotificationResponse(
                        saved.getId(),
                        saved.getTitle(),
                        saved.getMessage(),
                        saved.isReadStatus(),
                        saved.getCreatedAt(),
                        saved.getUserId(),
                        saved.getUserType(),
                        saved.getEventId(),
                        saved.getEventType()
                )
        );
        NotificationResponse response = new NotificationResponse(
                saved.getId(),
                saved.getTitle(),
                saved.getMessage(),
                saved.isReadStatus(),
                saved.getCreatedAt(),
                saved.getUserId(),
                saved.getUserType(),
                saved.getEventId(),
                saved.getEventType()
        );

        // 2. Envoyer via WebSocket
        messagingTemplate.convertAndSend(
                "/queue/notifications/" + userId,
                response
        );
    }

    // Marquer une notification comme lue
    public void marquerCommeLue(UUID notificationId, UUID userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification non trouvée"));

        // Vérifier que la notification appartient à cet utilisateur
        if (!notification.getUserId().equals(userId)) {
            throw new RuntimeException("Cette notification ne vous appartient pas");
        }

        notification.setReadStatus(true);
        notificationRepository.save(notification);
    }

    // Récupérer toutes les notifications d'un utilisateur
    public List<NotificationResponse> getNotifications(UUID userId) {

        // Vérifier que l'utilisateur existe dans la table users
        Optional<User> user = userRepository.findById(userId);
        if (user.isEmpty())
            throw new RuntimeException("Utilisateur non trouvé");


        return notificationRepository.findByUserIdAndUserType(userId, user.get().getRole())
                .stream()
                .map(notification -> new NotificationResponse(
                        notification.getId(),
                        notification.getTitle(),
                        notification.getMessage(),
                        notification.isReadStatus(),
                        notification.getCreatedAt(),
                        notification.getUserId(),
                        notification.getUserType(),
                        notification.getEventId(),
                        notification.getEventType()
                ))
                .toList();
    }
}