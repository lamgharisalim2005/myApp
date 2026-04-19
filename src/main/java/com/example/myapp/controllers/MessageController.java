package com.example.myapp.controllers;

import com.example.myapp.dtos.ConversationResponse;
import com.example.myapp.dtos.MessageResponse;
import com.example.myapp.dtos.SendMessageRequest;
import com.example.myapp.exceptions.GlobalResponse;
import com.example.myapp.services.MessageService;
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
    // TODO: senderId sera extrait du JWT après Spring Security
    @PostMapping
    public ResponseEntity<GlobalResponse<MessageResponse>> envoyerMessage(
            @RequestBody SendMessageRequest request) {
        MessageResponse response = messageService.envoyerMessage(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new GlobalResponse<>(response));
    }

    // CLIENT ET COIFFEUR — Envoyer un message via WebSocket
    @MessageMapping("/chat")
    public void envoyerMessageWebSocket(@Payload SendMessageRequest request) {
        messageService.envoyerMessage(request);
    }

    // CLIENT ET COIFFEUR — Voir une conversation
    // TODO: userId sera extrait du JWT après Spring Security
    @GetMapping("/conversation")
    public ResponseEntity<GlobalResponse<List<MessageResponse>>> getConversation(
            @RequestParam UUID user1,
            @RequestParam UUID user2,
            @RequestParam UUID currentUserId) {
        List<MessageResponse> messages = messageService.getConversation(user1, user2, currentUserId);
        return ResponseEntity.ok(new GlobalResponse<>(messages));
    }

    // CLIENT ET COIFFEUR — Marquer un message comme lu
    // TODO: userId sera extrait du JWT après Spring Security
    @PutMapping("/{messageId}/read")
    public ResponseEntity<GlobalResponse<String>> marquerCommeLu(
            @PathVariable UUID messageId,
            @RequestParam UUID userId) {
        messageService.marquerCommeLu(messageId, userId);
        return ResponseEntity.ok(new GlobalResponse<>("Message marqué comme lu"));
    }

    // CLIENT — Voir toutes ses conversations
    // TODO: clientId sera extrait du JWT après Spring Security
    @GetMapping("/conversations/client/{clientId}")
    public ResponseEntity<GlobalResponse<List<ConversationResponse>>> getConversationsClient(
            @PathVariable UUID clientId) {
        List<ConversationResponse> conversations = messageService.getConversationsClient(clientId);
        return ResponseEntity.ok(new GlobalResponse<>(conversations));
    }

    // COIFFEUR — Voir toutes ses conversations
    // TODO: coiffeurId sera extrait du JWT après Spring Security
    @GetMapping("/conversations/coiffeur/{coiffeurId}")
    public ResponseEntity<GlobalResponse<List<ConversationResponse>>> getConversationsCoiffeur(
            @PathVariable UUID coiffeurId) {
        List<ConversationResponse> conversations = messageService.getConversationsCoiffeur(coiffeurId);
        return ResponseEntity.ok(new GlobalResponse<>(conversations));
    }
}