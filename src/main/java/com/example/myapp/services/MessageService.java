package com.example.myapp.services;

import com.example.myapp.dtos.ConversationResponse;
import com.example.myapp.dtos.MessageResponse;
import com.example.myapp.dtos.SendMessageRequest;
import com.example.myapp.entitys.Client;
import com.example.myapp.entitys.Coiffeur;
import com.example.myapp.entitys.Message;
import com.example.myapp.repositories.ClientRepository;
import com.example.myapp.repositories.CoiffeurRepository;
import com.example.myapp.repositories.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    // SimpMessagingTemplate pour envoyer les messages en temps réel via WebSocket
    private final SimpMessagingTemplate messagingTemplate;
    private final CoiffeurRepository coiffeurRepository;
    private final ClientRepository clientRepository;

    // Envoyer un message
    public MessageResponse envoyerMessage(SendMessageRequest request) {

        // 1. Vérifier que l'expéditeur existe
        if (request.senderType().equals("CLIENT")) {
            clientRepository.findById(request.senderId())
                    .orElseThrow(() -> new RuntimeException("Client non trouvé"));
        } else if (request.senderType().equals("COIFFEUR")) {
            coiffeurRepository.findById(request.senderId())
                    .orElseThrow(() -> new RuntimeException("Coiffeur non trouvé"));
        }

        // 2. Vérifier que le destinataire existe
        if (request.receiverType().equals("CLIENT")) {
            clientRepository.findById(request.receiverId())
                    .orElseThrow(() -> new RuntimeException("Client non trouvé"));
        } else if (request.receiverType().equals("COIFFEUR")) {
            coiffeurRepository.findById(request.receiverId())
                    .orElseThrow(() -> new RuntimeException("Coiffeur non trouvé"));
        }

        // 3. Vérifier que l'expéditeur ne s'envoie pas un message à lui-même
        if (request.senderId().equals(request.receiverId())) {
            throw new RuntimeException("Vous ne pouvez pas vous envoyer un message à vous-même");
        }

        // 4. Vérifier que le contenu n'est pas vide
        if (request.content() == null || request.content().trim().isEmpty()) {
            throw new RuntimeException("Le contenu du message ne peut pas être vide");
        }

        // 5. Sauvegarder le message
        Message message = new Message();
        message.setSenderId(request.senderId());
        message.setSenderType(request.senderType());
        message.setReceiverId(request.receiverId());
        message.setReceiverType(request.receiverType());
        message.setContent(request.content());
        message.setCreatedAt(LocalDateTime.now());
        message.setStatus("SENT");
        Message saved = messageRepository.save(message);

        MessageResponse response = new MessageResponse(
                saved.getId(),
                saved.getSenderId(),
                saved.getSenderType(),
                saved.getReceiverId(),
                saved.getReceiverType(),
                saved.getContent(),
                saved.getCreatedAt(),
                saved.getStatus(),
                true // ← isMe = true pour l'expéditeur
        );

        // 6. Envoyer via WebSocket au destinataire
        messagingTemplate.convertAndSend(
                "/queue/messages/" + request.receiverId(),
                new MessageResponse(
                        saved.getId(),
                        saved.getSenderId(),
                        saved.getSenderType(),
                        saved.getReceiverId(),
                        saved.getReceiverType(),
                        saved.getContent(),
                        saved.getCreatedAt(),
                        saved.getStatus(),
                        false // ← isMe = false pour le destinataire
                )
        );

        return response;
    }

    // Voir une conversation
    public List<MessageResponse> getConversation(UUID user1, UUID user2, UUID currentUserId) {

        // 1. Vérifier que currentUserId est bien user1 ou user2
        if (!currentUserId.equals(user1) && !currentUserId.equals(user2)) {
            throw new RuntimeException("Vous n'êtes pas autorisé à voir cette conversation");
        }

        // 2. Vérifier que user1 et user2 sont différents
        if (user1.equals(user2)) {
            throw new RuntimeException("Vous ne pouvez pas avoir une conversation avec vous-même");
        }

        return messageRepository.findConversation(user1, user2)
                .stream()
                .map(message -> new MessageResponse(
                        message.getId(),
                        message.getSenderId(),
                        message.getSenderType(),
                        message.getReceiverId(),
                        message.getReceiverType(),
                        message.getContent(),
                        message.getCreatedAt(),
                        message.getStatus(),
                        message.getSenderId().equals(currentUserId) // ← isMe
                ))
                .toList();
    }

    // Marquer un message comme lu
    public void marquerCommeLu(UUID messageId, UUID userId) {

        // 1. Vérifier que le message existe
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message non trouvé"));

        // 2. Vérifier que c'est bien le destinataire
        if (!message.getReceiverId().equals(userId)) {
            throw new RuntimeException("Vous n'êtes pas le destinataire de ce message");
        }

        // 3. Vérifier que le message n'est pas déjà lu
        if (message.getStatus().equals("READ")) {
            throw new RuntimeException("Ce message est déjà marqué comme lu");
        }

        // 4. Marquer comme lu
        message.setStatus("READ");
        messageRepository.save(message);

        // 5. Notifier l'expéditeur via WebSocket
        messagingTemplate.convertAndSend(
                "/queue/messages/" + message.getSenderId(),
                new MessageResponse(
                        message.getId(),
                        message.getSenderId(),
                        message.getSenderType(),
                        message.getReceiverId(),
                        message.getReceiverType(),
                        message.getContent(),
                        message.getCreatedAt(),
                        message.getStatus(),
                        false // ← isMe = false
                )
        );
    }

    // Voir les conversations d'un client
    public List<ConversationResponse> getConversationsClient(UUID clientId) {

        // 1. Vérifier que le client existe
        clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client non trouvé"));

        List<UUID> partnerIds = messageRepository.findConversationPartners(clientId);

        return partnerIds.stream()
                .map(partnerId -> {
                    Message lastMessage = messageRepository
                            .findLastMessage(clientId, partnerId)
                            .orElse(null);

                    Coiffeur coiffeur = coiffeurRepository.findById(partnerId)
                            .orElse(null);

                    if (coiffeur == null || lastMessage == null) return null;

                    // Dans getConversationsClient
                    return new ConversationResponse(
                            coiffeur.getId(),
                            coiffeur.getUser().getName(), // ← via User
                            "COIFFEUR",
                            lastMessage.getContent(),
                            lastMessage.getCreatedAt()
                    );
                })
                .filter(c -> c != null)
                .toList();
    }

    // Voir les conversations d'un coiffeur
    public List<ConversationResponse> getConversationsCoiffeur(UUID coiffeurId) {

        // 1. Vérifier que le coiffeur existe
        coiffeurRepository.findById(coiffeurId)
                .orElseThrow(() -> new RuntimeException("Coiffeur non trouvé"));

        List<UUID> partnerIds = messageRepository.findConversationPartners(coiffeurId);

        return partnerIds.stream()
                .map(partnerId -> {
                    Message lastMessage = messageRepository
                            .findLastMessage(coiffeurId, partnerId)
                            .orElse(null);

                    Client client = clientRepository.findById(partnerId)
                            .orElse(null);

                    if (client == null || lastMessage == null) return null;

                    return new ConversationResponse(
                            client.getId(),
                            client.getUser().getName(), // ← via User
                            "CLIENT",
                            lastMessage.getContent(),
                            lastMessage.getCreatedAt()
                    );
                })
                .filter(c -> c != null)
                .toList();
    }
}