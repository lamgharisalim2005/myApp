package com.example.myapp.services;

import com.example.myapp.dtos.PaymentIntentResponse;
import com.example.myapp.dtos.PaymentRequest;
import com.example.myapp.entitys.Payment;
import com.example.myapp.entitys.Reservation;
import com.example.myapp.repositories.ClientRepository;
import com.example.myapp.repositories.PaymentRepository;
import com.example.myapp.repositories.ReservationRepository;
import com.stripe.model.PaymentIntent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final StripeService stripeService;
    private final PaymentRepository paymentRepository;
    private final ReservationRepository reservationRepository;
    private final NotificationService notificationService;
    private final ClientRepository clientRepository;

    public PaymentIntentResponse creerPaymentIntent(PaymentRequest request) {

        // 1. Vérifier que le client existe
        clientRepository.findById(request.clientId())
                .orElseThrow(() -> new RuntimeException("Client non trouvé"));

        // 2. Vérifier que la réservation existe et est en WAITING_PAYMENT
        Reservation reservation = reservationRepository.findById(request.reservationId())
                .orElseThrow(() -> new RuntimeException("Réservation non trouvée"));

        // Vérifier que la réservation n'est pas dans le passé
        if (reservation.getStartTime().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Vous ne pouvez pas payer une réservation passée");
        }

        if (!reservation.getStatus().equals("WAITING_PAYMENT")) {
            throw new RuntimeException("Cette réservation n'est pas en attente de paiement");
        }

        // 3. Vérifier que la réservation appartient à ce client ← nouveau
        if (!reservation.getClient().getId().equals(request.clientId())) {
            throw new RuntimeException("Cette réservation ne vous appartient pas");
        }

        // 4. Vérifier qu'il n'y a pas déjà un paiement
        if (paymentRepository.findByReservationId(reservation.getId()).isPresent()) {
            throw new RuntimeException("Un paiement existe déjà pour cette réservation");
        }

        Double montant = reservation.getServices()
                .stream()
                .mapToDouble(com.example.myapp.entitys.Service::getPrice)
                .sum();

        PaymentIntent paymentIntent = stripeService.creerPaymentIntent(montant, request.currency());

        Payment payment = new Payment();
        payment.setReservation(reservation);
        payment.setAmount(montant);
        payment.setStatus("PENDING");
        payment.setMethod("STRIPE");
        payment.setTransactionId(paymentIntent.getId());
        payment.setCreatedAt(LocalDateTime.now());
        paymentRepository.save(payment);

        return new PaymentIntentResponse(
                paymentIntent.getClientSecret(),
                paymentIntent.getId(),
                montant,
                request.currency()
        );
    }

    public void confirmerPaiement(String paymentIntentId) {

        PaymentIntent paymentIntent = stripeService.recupererPaymentIntent(paymentIntentId);

        Payment payment = paymentRepository.findByTransactionId(paymentIntentId)
                .orElseThrow(() -> new RuntimeException("Paiement non trouvé"));

        if (!payment.getStatus().equals("PENDING")) {
            return;
        }

        Reservation reservation = payment.getReservation();

        String nomsServices = reservation.getServices()
                .stream()
                .map(com.example.myapp.entitys.Service::getName)
                .collect(Collectors.joining(", "));

        if (paymentIntent.getStatus().equals("succeeded")) {
            payment.setStatus("SUCCESS");
            paymentRepository.save(payment);

            reservation.setStatus("CONFIRMED");
            reservationRepository.save(reservation);

            // Notifier le coiffeur
            notificationService.envoyerNotification(
                    reservation.getCoiffeur().getId(),
                    "COIFFEUR",
                    "Paiement reçu",
                    "Le client " + reservation.getClient().getName() +
                            " a payé pour " + nomsServices,
                    reservation.getId(),  // ← eventId
                    "RESERVATION"         // ← eventType
            );

            // Notifier le client
            notificationService.envoyerNotification(
                    reservation.getClient().getId(),
                    "CLIENT",
                    "Paiement confirmé",
                    "Votre paiement pour " + nomsServices +
                            " a été confirmé avec succès",
                    reservation.getId(),  // ← eventId
                    "RESERVATION"         // ← eventType
            );

        } else {
            payment.setStatus("FAILED");
            paymentRepository.save(payment);

            // Notifier le client
            notificationService.envoyerNotification(
                    reservation.getClient().getId(),
                    "CLIENT",
                    "Paiement échoué",
                    "Votre paiement pour " + nomsServices +
                            " a échoué. Veuillez réessayer.",
                    reservation.getId(),  // ← eventId
                    "PAYMENT"             // ← eventType
            );
        }
    }
}