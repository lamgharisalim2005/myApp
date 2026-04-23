package com.example.myapp.controllers;

import com.example.myapp.dtos.NotificationResponse;
import com.example.myapp.exceptions.GlobalResponse;
import com.example.myapp.services.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
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

    // CLIENT ET COIFFEUR — Voir ses notifications
    // userId extrait automatiquement du JWT
    @GetMapping("/user")
    public ResponseEntity<GlobalResponse<List<NotificationResponse>>> getNotificationsClient(
            HttpServletRequest httpRequest) {
        UUID userId = (UUID) httpRequest.getAttribute("userId");
        List<NotificationResponse> notifications = notificationService.getNotifications(userId);
        return ResponseEntity.ok(new GlobalResponse<>(notifications));
    }


    // CLIENT ET COIFFEUR — Marquer une notification comme lue
    // userId extrait automatiquement du JWT
    @PutMapping("/{notificationId}/read")
    public ResponseEntity<GlobalResponse<String>> marquerCommeLue(
            @PathVariable UUID notificationId,
            HttpServletRequest httpRequest) {
        UUID userId = (UUID) httpRequest.getAttribute("userId");
        notificationService.marquerCommeLue(notificationId, userId);
        return ResponseEntity.ok(new GlobalResponse<>("Notification marquée comme lue"));
    }
}