package com.example.myapp.controllers;

import com.example.myapp.dtos.NotificationResponse;
import com.example.myapp.exceptions.GlobalResponse;
import com.example.myapp.services.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class NotificationController {

    private final NotificationService notificationService;

    // CLIENT — Voir ses notifications
    @GetMapping("/client/{userId}")
    public ResponseEntity<GlobalResponse<List<NotificationResponse>>> getNotificationsClient(
            @PathVariable UUID userId) {
        List<NotificationResponse> notifications = notificationService.getNotifications(userId, "CLIENT");
        return ResponseEntity.ok(new GlobalResponse<>(notifications));
    }

    // COIFFEUR — Voir ses notifications
    @GetMapping("/coiffeur/{userId}")
    public ResponseEntity<GlobalResponse<List<NotificationResponse>>> getNotificationsCoiffeur(
            @PathVariable UUID userId) {
        List<NotificationResponse> notifications = notificationService.getNotifications(userId, "COIFFEUR");
        return ResponseEntity.ok(new GlobalResponse<>(notifications));
    }

    // CLIENT ET COIFFEUR — Marquer une notification comme lue
    @PutMapping("/{notificationId}/read")
    public ResponseEntity<GlobalResponse<String>> marquerCommeLue(
            @PathVariable UUID notificationId,
            @RequestParam UUID userId) {
        notificationService.marquerCommeLue(notificationId, userId);
        return ResponseEntity.ok(new GlobalResponse<>("Notification marquée comme lue"));
    }
}