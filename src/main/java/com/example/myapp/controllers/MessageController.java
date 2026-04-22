package com.example.myapp.controllers;

import com.example.myapp.dtos.ConversationResponse;
import com.example.myapp.dtos.MessageResponse;
import com.example.myapp.dtos.SendMessageRequest;
import com.example.myapp.exceptions.GlobalResponse;
import com.example.myapp.services.MessageService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MessageController {

    private final MessageService messageService;

    // CLIENT ET COIFFEUR — Envoyer un message via REST → 201 Created
    // userId extrait automatiquement du JWT
    @PostMapping
    public ResponseEntity<GlobalResponse<MessageResponse>> envoyerMessage(
            @RequestBody SendMessageRequest request,
            HttpServletRequest httpRequest) {
        UUID userId = (UUID) httpRequest.getAttribute("userId");
        MessageResponse response = messageService.envoyerMessage(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(new GlobalResponse<>(response));
    }

    // CLIENT ET COIFFEUR — Envoyer un message via WebSocket
    @MessageMapping("/chat")
    public void envoyerMessageWebSocket(@Payload SendMessageRequest request) {
        messageService.envoyerMessage(request, null);
    }

    // CLIENT ET COIFFEUR — Voir une conversation
    // userId extrait automatiquement du JWT
    @GetMapping("/conversation")
    public ResponseEntity<GlobalResponse<List<MessageResponse>>> getConversation(
            @RequestParam UUID otherUserId,
            HttpServletRequest httpRequest) {
        UUID userId = (UUID) httpRequest.getAttribute("userId");
        List<MessageResponse> messages = messageService.getConversation(userId, otherUserId);
        return ResponseEntity.ok(new GlobalResponse<>(messages));
    }

    // CLIENT ET COIFFEUR — Marquer un message comme lu
    // userId extrait automatiquement du JWT
    @PutMapping("/{messageId}/read")
    public ResponseEntity<GlobalResponse<String>> marquerCommeLu(
            @PathVariable UUID messageId,
            HttpServletRequest httpRequest) {
        UUID userId = (UUID) httpRequest.getAttribute("userId");
        messageService.marquerCommeLu(messageId, userId);
        return ResponseEntity.ok(new GlobalResponse<>("Message marqué comme lu"));
    }

    // CLIENT — Voir toutes ses conversations
    // userId extrait automatiquement du JWT
    @GetMapping("/conversations")
    public ResponseEntity<GlobalResponse<List<ConversationResponse>>> getConversations(
            HttpServletRequest httpRequest) {
        UUID userId = (UUID) httpRequest.getAttribute("userId");
        String role = (String) httpRequest.getAttribute("role");
        List<ConversationResponse> conversations = messageService.getConversations(userId, role);
        return ResponseEntity.ok(new GlobalResponse<>(conversations));
    }
}