package com.example.myapp.services;

import com.example.myapp.dtos.*;
import com.example.myapp.entitys.*;
import com.example.myapp.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final ClientRepository clientRepository;
    private final CoiffeurRepository coiffeurRepository;
    private final ServiceRepository serviceRepository;
    private final WorkScheduleRepository workScheduleRepository;
    private final NotificationService notificationService;

    public ReservationResponse creerReservation(CreateReservationRequest request, UUID userId) {

        Client client = clientRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User non trouvé"));

        Coiffeur coiffeur = coiffeurRepository.findByUserId(request.coiffeurId())
                .orElseThrow(() -> new RuntimeException("Coiffeur non trouvé"));

        List<com.example.myapp.entitys.Service> services = request.serviceIds()
                .stream()
                .map(serviceId -> {
                    com.example.myapp.entitys.Service service = serviceRepository.findById(serviceId)
                            .orElseThrow(() -> new RuntimeException("Service non trouvé : " + serviceId));
                    if (!service.getCoiffeur().getId().equals(coiffeur.getId())) {
                        throw new RuntimeException("Le service " + service.getName() + " n'appartient pas à ce coiffeur");
                    }
                    return service;
                })
                .toList();

        LocalDateTime startTime = request.startTime();

        if (startTime.isBefore(LocalDateTime.now())) {
            throw new RuntimeException("La date de réservation doit être dans le futur");
        }

        int dureeTotale = services.stream()
                .mapToInt(com.example.myapp.entitys.Service::getDuration)
                .sum();
        LocalDateTime endTime = startTime.plusMinutes(dureeTotale);

        String dayOfWeek = startTime.getDayOfWeek().name();
        LocalTime heureDebut = startTime.toLocalTime();
        LocalTime heureFin = endTime.toLocalTime();

        List<WorkSchedule> schedules = workScheduleRepository
                .findByCoiffeurIdAndDayOfWeek(coiffeur.getId(), dayOfWeek);

        boolean travailleCeJour = schedules.stream().anyMatch(ws ->
                !heureDebut.isBefore(ws.getStartTime()) &&
                        !heureFin.isAfter(ws.getEndTime())
        );

        if (!travailleCeJour) {
            throw new RuntimeException("Le coiffeur ne travaille pas à ce créneau");
        }

        // Vérifier que le coiffeur n'a pas déjà un créneau CONFIRMED ou WAITING_PAYMENT
        if (reservationRepository.existsConflict(coiffeur.getId(), startTime, endTime)) {
            throw new RuntimeException("Ce créneau est déjà confirmé par le coiffeur");
        }

        // Vérifier que le client n'a pas déjà une réservation qui chevauche ce créneau
        if (reservationRepository.existsClientConflict(client.getId(), startTime, endTime)) {
            throw new RuntimeException("Vous avez déjà une réservation active sur ce créneau");
        }

        Reservation reservation = new Reservation();
        reservation.setClient(client);
        reservation.setCoiffeur(coiffeur);
        reservation.setServices(services);
        reservation.setStartTime(startTime);
        reservation.setEndTime(endTime);
        reservation.setStatus("PENDING");
        reservation.setCreatedAt(LocalDateTime.now());
        Reservation saved = reservationRepository.save(reservation);

        String nomsServices = services.stream()
                .map(com.example.myapp.entitys.Service::getName)
                .collect(Collectors.joining(", "));

        notificationService.envoyerNotification(
                coiffeur.getUser().getId(),
                "Nouvelle demande de réservation",
                "Le client " + client.getUser().getName() + " veut réserver " +
                        nomsServices + " le " + dayOfWeek + " à " + heureDebut,
                saved.getId(),
                "RESERVATION"
        );

        return mapToResponse(saved);
    }

    public ReservationResponse traiterReservation(UUID reservationId, String decision, UUID userId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("Réservation non trouvée"));

        Coiffeur coiffeur = coiffeurRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Coiffeur non trouvé"));

        if (!reservation.getCoiffeur().getId().equals(coiffeur.getId())) {
            throw new RuntimeException("Cette réservation ne vous appartient pas");
        }

        if (!reservation.getStatus().equals("PENDING")) {
            throw new RuntimeException("Cette réservation n'est plus en attente");
        }

        // ✅ CONFIRMED ou REJECTED — plus CANCELLED
        if (!decision.equals("CONFIRMED") && !decision.equals("REJECTED")) {
            throw new RuntimeException("Décision invalide — utilisez CONFIRMED ou REJECTED");
        }

        String nomsServices = reservation.getServices()
                .stream()
                .map(com.example.myapp.entitys.Service::getName)
                .collect(Collectors.joining(", "));

        if (decision.equals("CONFIRMED")) {
            reservation.setStatus("WAITING_PAYMENT");
            reservationRepository.save(reservation);

            // Supprimer toutes les autres réservations PENDING qui chevauchent ce créneau
            List<Reservation> autresReservations = reservationRepository
                    .findConflictingPendingReservations(
                            coiffeur.getId(),
                            reservation.getId(),
                            reservation.getStartTime(),
                            reservation.getEndTime()
                    );

            for (Reservation autre : autresReservations) {
                String nomsServicesAutre = autre.getServices()
                        .stream()
                        .map(com.example.myapp.entitys.Service::getName)
                        .collect(Collectors.joining(", "));

                // Notifier le client que sa réservation est annulée
                notificationService.envoyerNotification(
                        autre.getClient().getUser().getId(),
                        "Réservation annulée",
                        "Votre réservation pour " + nomsServicesAutre +
                                " a été annulée car un autre client a été confirmé sur ce créneau.",
                        autre.getId(),
                        "RESERVATION"
                );

                reservationRepository.delete(autre);
            }

            notificationService.envoyerNotification(
                    reservation.getClient().getUser().getId(),
                    "Réservation confirmée",
                    "Votre réservation pour " + nomsServices +
                            " a été confirmée. Veuillez procéder au paiement.",
                    reservation.getId(),
                    "RESERVATION"
            );
        }

        // ✅ REJECTED — le client ne peut pas recréer au même créneau
        if (decision.equals("REJECTED")) {
            reservation.setStatus("REJECTED");
            reservationRepository.save(reservation);

            notificationService.envoyerNotification(
                    reservation.getClient().getUser().getId(),
                    "Réservation refusée",
                    "Votre réservation pour " + nomsServices +
                            " a été refusée par le coiffeur.",
                    reservation.getId(),
                    "RESERVATION"
            );
        }

        return mapToResponse(reservation);
    }

    @Transactional
    @Scheduled(fixedRate = 60000)
    public void completerReservations() {
        List<Reservation> reservations = reservationRepository
                .findReservationsACompleter(LocalDateTime.now());

        reservations.forEach(reservation -> {
            reservation.setStatus("COMPLETED");
            reservationRepository.save(reservation);

            String nomsServices = reservation.getServices()
                    .stream()
                    .map(com.example.myapp.entitys.Service::getName)
                    .collect(Collectors.joining(", "));

            notificationService.envoyerNotification(
                    reservation.getClient().getUser().getId(),
                    "Service terminé",
                    "Votre service " + nomsServices + " est terminé.",
                    reservation.getId(),
                    "RESERVATION"
            );
        });
    }

    public List<ReservationResponse> getReservationsClient(UUID userId) {
        Client client = clientRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Client non trouvé"));

        return reservationRepository.findByClientId(client.getId())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<ReservationResponse> getReservationsCoiffeur(UUID userId) {
        Coiffeur coiffeur = coiffeurRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Coiffeur non trouvé"));

        return reservationRepository.findByCoiffeurId(coiffeur.getId())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private ReservationResponse mapToResponse(Reservation reservation) {
        List<String> serviceNames = reservation.getServices()
                .stream()
                .map(com.example.myapp.entitys.Service::getName)
                .toList();

        Double totalPrice = reservation.getServices()
                .stream()
                .mapToDouble(com.example.myapp.entitys.Service::getPrice)
                .sum();

        return new ReservationResponse(
                reservation.getId(),
                reservation.getStatus(),
                reservation.getStartTime(),
                reservation.getEndTime(),
                reservation.getClient().getUser().getName(),
                reservation.getCoiffeur().getUser().getName(),
                serviceNames,
                totalPrice
        );
    }

    public List<SlotResponse> getConfirmedSlots(UUID coiffeurId, UUID userId) {
        Coiffeur coiffeur = coiffeurRepository.findByUserId(coiffeurId)
                .orElseThrow(() -> new RuntimeException("Coiffeur non trouvé"));

        // 1. Slots occupés pour tout le monde (CONFIRMED + WAITING_PAYMENT)
        List<Reservation> confirmedSlots = reservationRepository
                .findByCoiffeurIdAndStatusInAndStartTimeAfter(
                        coiffeur.getId(),
                        List.of("CONFIRMED", "WAITING_PAYMENT"),
                        LocalDateTime.now()
                );

        // 2. Slots occupés uniquement pour ce client (PENDING + REJECTED)
        Client client = clientRepository.findByUserId(userId).orElse(null);

        List<Reservation> clientSlots = new java.util.ArrayList<>();
        if (client != null) {
            clientSlots = reservationRepository
                    .findByCoiffeurIdAndClientIdAndStatusInAndStartTimeAfter(
                            coiffeur.getId(),
                            client.getId(),
                            List.of("PENDING", "REJECTED"),
                            LocalDateTime.now()
                    );
        }

        // 3. Combiner les deux listes
        List<SlotResponse> result = new java.util.ArrayList<>();
        confirmedSlots.forEach(r ->
                result.add(new SlotResponse(r.getStartTime(), r.getEndTime())));
        clientSlots.forEach(r ->
                result.add(new SlotResponse(r.getStartTime(), r.getEndTime())));

        return result;
    }

    public ReservationResponse annulerReservation(UUID reservationId, UUID userId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("Réservation non trouvée"));

        Client client = clientRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Client non trouvé"));

        if (!reservation.getClient().getId().equals(client.getId())) {
            throw new RuntimeException("Cette réservation ne vous appartient pas");
        }

        if (!reservation.getStatus().equals("PENDING")) {
            throw new RuntimeException("Vous ne pouvez annuler qu'une réservation en attente");
        }

        reservation.setStatus("CANCELLED");
        reservationRepository.save(reservation);

        notificationService.envoyerNotification(
                reservation.getCoiffeur().getUser().getId(),
                "Réservation annulée",
                "Le client " + client.getUser().getName() + " a annulé sa réservation.",
                reservation.getId(),
                "RESERVATION"
        );

        return mapToResponse(reservation);
    }

    public void supprimerReservation(UUID reservationId, UUID userId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("Réservation non trouvée"));

        boolean isClient = clientRepository.findByUserId(userId)
                .map(c -> c.getId().equals(reservation.getClient().getId()))
                .orElse(false);

        boolean isCoiffeur = coiffeurRepository.findByUserId(userId)
                .map(c -> c.getId().equals(reservation.getCoiffeur().getId()))
                .orElse(false);

        if (!isClient && !isCoiffeur) {
            throw new RuntimeException("Vous n'avez pas le droit de supprimer cette réservation");
        }

        // ✅ REJECTED ne peut pas être supprimée
        List<String> passeStatuses = List.of("COMPLETED", "CANCELLED");
        if (!passeStatuses.contains(reservation.getStatus())) {
            throw new RuntimeException("Vous ne pouvez supprimer qu'une réservation passée");
        }

        reservationRepository.delete(reservation);
    }
}