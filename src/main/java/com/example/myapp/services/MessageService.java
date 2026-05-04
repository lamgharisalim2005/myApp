package com.example.myapp.services;

import com.example.myapp.configs.WebSocketEventListener;
import com.example.myapp.dtos.ConversationResponse;
import com.example.myapp.dtos.MessageResponse;
import com.example.myapp.dtos.SendMessageRequest;
import com.example.myapp.entitys.Client;
import com.example.myapp.entitys.Coiffeur;
import com.example.myapp.entitys.Message;
import com.example.myapp.entitys.User;
import com.example.myapp.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final CoiffeurRepository coiffeurRepository;
    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final BlockRepository blockRepository;

    // ✅ Envoyer un message — userId extrait du JWT
    public MessageResponse envoyerMessage(SendMessageRequest request, UUID userId) {

        // 1. Vérifier que l'expéditeur existe
        Optional<User> sender = userRepository.findById(userId);
        if (sender.isEmpty())
            throw new RuntimeException("sender non trouvé");

        // 2. Vérifier que le destinataire existe
        Optional<User> reciever = userRepository.findById(request.receiverId());
        if (reciever.isEmpty()) {
            throw new RuntimeException("reciever non trouvé");
        }

        // 3. Vérifier que l'expéditeur ne s'envoie pas un message à lui-même
        if (sender.get().getId().equals(reciever.get().getId())) {
            throw new RuntimeException("Vous ne pouvez pas vous envoyer un message à vous-même");
        }

        // 4. Vérifier que le contenu n'est pas vide
        if (request.content() == null || request.content().trim().isEmpty()) {
            throw new RuntimeException("Le contenu du message ne peut pas être vide");
        }

        // Vérifier si l'expéditeur est bloqué par le destinataire
        if (blockRepository.existsByBlockerIdAndBlockedId(request.receiverId(), userId)) {
            throw new RuntimeException("Vous ne pouvez pas envoyer de message à cet utilisateur");
        }

        // 5. Sauvegarder le message avec statut SENT
        Message message = new Message();
        message.setSenderId(sender.get().getId());
        message.setSenderType(sender.get().getRole());
        message.setReceiverId(reciever.get().getId());
        message.setReceiverType(reciever.get().getRole());
        message.setContent(request.content());
        message.setCreatedAt(LocalDateTime.now());
        message.setStatus("SENT");
        Message saved = messageRepository.save(message);

        // 6. Vérifier si le destinataire est en ligne
        boolean receiverOnline = WebSocketEventListener.onlineUsers
                .containsKey(request.receiverId());

        // 7. Si destinataire en ligne → DELIVERED, sinon → SENT
        String finalStatus = receiverOnline ? "DELIVERED" : "SENT";
        if (receiverOnline) {
            saved.setStatus("DELIVERED");
            messageRepository.save(saved);
        }

        // 8. Envoyer via WebSocket au destinataire
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
                        finalStatus,
                        false,
                        sender.get().getProfilePicture()
                )
        );

        // 9. Notifier l'expéditeur du statut final
        messagingTemplate.convertAndSend(
                "/queue/messages/" + userId,
                new MessageResponse(
                        saved.getId(),
                        saved.getSenderId(),
                        saved.getSenderType(),
                        saved.getReceiverId(),
                        saved.getReceiverType(),
                        saved.getContent(),
                        saved.getCreatedAt(),
                        finalStatus,
                        true,
                        sender.get().getProfilePicture()
                )
        );

        // 10. Retourner la réponse à l'expéditeur
        return new MessageResponse(
                saved.getId(),
                saved.getSenderId(),
                saved.getSenderType(),
                saved.getReceiverId(),
                saved.getReceiverType(),
                saved.getContent(),
                saved.getCreatedAt(),
                finalStatus,
                true,
                sender.get().getProfilePicture()
        );
    }

    // ✅ Voir une conversation — userId extrait du JWT
    public List<MessageResponse> getConversation(UUID userId, UUID otherUserId) {

        // Vérifier que les deux utilisateurs sont différents
        if (userId.equals(otherUserId)) {
            throw new RuntimeException("Vous ne pouvez pas avoir une conversation avec vous-même");
        }

        return messageRepository.findConversation(userId, otherUserId)
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
                        message.getSenderId().equals(userId),
                        userRepository.findById(message.getSenderId()).get().getProfilePicture()
                ))
                .toList();
    }

    // ✅ Marquer un message comme lu — userId extrait du JWT
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
                        false,
                        userRepository.findById(message.getSenderId()).get().getProfilePicture()
                )
        );
    }

    // ✅ Voir toutes les conversations — unifié pour CLIENT et COIFFEUR
    public List<ConversationResponse> getConversations(UUID userId) {
        List<UUID> partnerIds = messageRepository.findConversationPartners(userId);

        return partnerIds.stream()
                .map(partnerId -> {
                    Message lastMessage = messageRepository
                            .findLastMessage(userId, partnerId)
                            .orElse(null);

                    if (lastMessage == null) return null;

                    String partnerName = userRepository.findById(partnerId).get().getName();
                    String partnerType;

                    if (userRepository.findById(userId).get().getRole().equals("CLIENT")) {
                        partnerType = "COIFFEUR";
                        partnerName = userRepository.findById(partnerId).get().getName();
                    } else {
                        partnerType = "CLIENT";
                        partnerName = userRepository.findById(partnerId).get().getName();
                    }

                    int unreadCount = messageRepository.countUnreadMessages(userId, partnerId);

                    return new ConversationResponse(
                            partnerId,
                            partnerName,
                            partnerType,
                            lastMessage.getContent(),
                            lastMessage.getCreatedAt(),
                            userRepository.findById(partnerId).get().getProfilePicture(),
                            unreadCount
                    );
                })
                .filter(c -> c != null)
                .toList();
    }
}