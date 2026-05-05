package com.example.myapp.services;

import com.example.myapp.configs.WebSocketEventListener;
import com.example.myapp.dtos.ConversationResponse;
import com.example.myapp.dtos.MessageResponse;
import com.example.myapp.dtos.SendMessageRequest;
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

    // Envoyer un message
    public MessageResponse envoyerMessage(SendMessageRequest request, UUID userId) {

        Optional<User> sender = userRepository.findById(userId);
        if (sender.isEmpty())
            throw new RuntimeException("sender non trouvé");

        Optional<User> receiver = userRepository.findById(request.receiverId());
        if (receiver.isEmpty())
            throw new RuntimeException("receiver non trouvé");

        if (sender.get().getId().equals(receiver.get().getId()))
            throw new RuntimeException("Vous ne pouvez pas vous envoyer un message à vous-même");

        if (request.content() == null || request.content().trim().isEmpty())
            throw new RuntimeException("Le contenu du message ne peut pas être vide");

        if (blockRepository.existsByBlockerIdAndBlockedId(request.receiverId(), userId))
            throw new RuntimeException("Vous ne pouvez pas envoyer de message à cet utilisateur");

        Message message = new Message();
        message.setSenderId(sender.get().getId());
        message.setSenderType(sender.get().getRole());
        message.setReceiverId(receiver.get().getId());
        message.setReceiverType(receiver.get().getRole());
        message.setContent(request.content());
        message.setCreatedAt(LocalDateTime.now());
        message.setStatus("SENT");
        Message saved = messageRepository.save(message);

        boolean receiverOnline = WebSocketEventListener.onlineUsers
                .containsKey(request.receiverId());

        String finalStatus = receiverOnline ? "DELIVERED" : "SENT";
        if (receiverOnline) {
            saved.setStatus("DELIVERED");
            messageRepository.save(saved);
        }

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

    // Voir une conversation
    public List<MessageResponse> getConversation(UUID userId, UUID otherUserId) {
        if (userId.equals(otherUserId))
            throw new RuntimeException("Vous ne pouvez pas avoir une conversation avec vous-même");

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

    // Marquer un message comme lu
    public void marquerCommeLu(UUID messageId, UUID userId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message non trouvé"));

        if (!message.getReceiverId().equals(userId))
            throw new RuntimeException("Vous n'êtes pas le destinataire de ce message");

        if (message.getStatus().equals("READ"))
            throw new RuntimeException("Ce message est déjà marqué comme lu");

        message.setStatus("READ");
        messageRepository.save(message);

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

    // Voir toutes les conversations — fix coiffeur ↔ coiffeur
    public List<ConversationResponse> getConversations(UUID userId) {
        List<UUID> partnerIds = messageRepository.findConversationPartners(userId);

        // Récupérer le role de l'utilisateur connecté
        String myRole = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"))
                .getRole();

        return partnerIds.stream()
                .map(partnerId -> {
                    Message lastMessage = messageRepository
                            .findLastMessage(userId, partnerId)
                            .orElse(null);

                    // Si pas de message visible → conversation supprimée → on cache
                    if (lastMessage == null) return null;

                    User partner = userRepository.findById(partnerId)
                            .orElse(null);
                    if (partner == null) return null;

                    // Fix : on prend le vrai role du partenaire
                    String partnerType = partner.getRole();

                    int unreadCount = messageRepository.countUnreadMessages(userId, partnerId);

                    return new ConversationResponse(
                            partnerId,
                            partner.getName(),
                            partnerType,
                            lastMessage.getContent(),
                            lastMessage.getCreatedAt(),
                            partner.getProfilePicture(),
                            unreadCount
                    );
                })
                .filter(c -> c != null)
                .toList();
    }

    // Supprimer une conversation — uniquement de mon côté
    public void supprimerConversation(UUID userId, UUID partnerId) {
        LocalDateTime now = LocalDateTime.now();

        // Marquer comme supprimé côté sender (mes messages envoyés)
        messageRepository.deleteConversationAsSender(userId, partnerId, now);

        // Marquer comme supprimé côté receiver (messages reçus)
        messageRepository.deleteConversationAsReceiver(userId, partnerId, now);
    }

    // Supprimer un message individuel — uniquement de mon côté
    public void supprimerMessage(UUID messageId, UUID userId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message non trouvé"));

        LocalDateTime now = LocalDateTime.now();

        if (message.getSenderId().equals(userId)) {
            // Tu es l'expéditeur
            messageRepository.deleteMessageAsSender(messageId, userId, now);
        } else if (message.getReceiverId().equals(userId)) {
            // Tu es le destinataire
            messageRepository.deleteMessageAsReceiver(messageId, userId, now);
        } else {
            throw new RuntimeException("Vous n'êtes pas autorisé à supprimer ce message");
        }
    }
}