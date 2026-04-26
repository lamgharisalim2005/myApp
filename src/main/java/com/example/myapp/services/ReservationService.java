package com.example.myapp.services;

import com.example.myapp.dtos.*;
import com.example.myapp.entitys.*;
import com.example.myapp.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

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

    // ✅ creerReservation — utilise userId
    public ReservationResponse creerReservation(CreateReservationRequest request, UUID userId) {

        // Chercher le client par userId
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

        if (reservationRepository.existsConflict(request.coiffeurId(), startTime, endTime)) {
            throw new RuntimeException("Ce créneau est déjà réservé");
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

        // ✅ utilise getUser().getId() pour les notifications
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

    // ✅ traiterReservation — utilise userId
    public ReservationResponse traiterReservation(UUID reservationId, String decision, UUID userId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("Réservation non trouvée"));

        // Chercher le coiffeur par userId
        Coiffeur coiffeur = coiffeurRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Coiffeur non trouvé"));

        // Vérifier que la réservation appartient à ce coiffeur
        if (!reservation.getCoiffeur().getId().equals(coiffeur.getId())) {
            throw new RuntimeException("Cette réservation ne vous appartient pas");
        }

        if (!reservation.getStatus().equals("PENDING")) {
            throw new RuntimeException("Cette réservation n'est plus en attente");
        }

        if (!decision.equals("CONFIRMED") && !decision.equals("CANCELLED")) {
            throw new RuntimeException("Décision invalide — utilisez CONFIRMED ou CANCELLED");
        }

        String nomsServices = reservation.getServices()
                .stream()
                .map(com.example.myapp.entitys.Service::getName)
                .collect(Collectors.joining(", "));

        if (decision.equals("CONFIRMED")) {
            reservation.setStatus("WAITING_PAYMENT");
            reservationRepository.save(reservation);

            // ✅ utilise getUser().getId() pour les notifications
            notificationService.envoyerNotification(
                    reservation.getClient().getUser().getId(),
                    "Réservation confirmée",
                    "Votre réservation pour " + nomsServices +
                            " a été confirmée. Veuillez procéder au paiement.",
                    reservation.getId(),
                    "RESERVATION"
            );
        }

        if (decision.equals("CANCELLED")) {
            reservation.setStatus("CANCELLED");
            reservationRepository.save(reservation);

            // ✅ utilise getUser().getId() pour les notifications
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

    // Scheduler — passe automatiquement à COMPLETED quand endTime est dépassé
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

            // ✅ utilise getUser().getId() pour les notifications
            notificationService.envoyerNotification(
                    reservation.getClient().getUser().getId(),
                    "Service terminé",
                    "Votre service " + nomsServices + " est terminé.",
                    reservation.getId(),
                    "RESERVATION"
            );
        });
    }

    // ✅ getReservationsClient — utilise userId
    public List<ReservationResponse> getReservationsClient(UUID userId) {
        Client client = clientRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Client non trouvé"));

        return reservationRepository.findByClientId(client.getId())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // ✅ getReservationsCoiffeur — utilise userId
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

    public List<SlotResponse> getConfirmedSlots(UUID coiffeurId) {

        Coiffeur coiffeur = coiffeurRepository.findByUserId(coiffeurId)
                .orElseThrow(() -> new RuntimeException("Coiffeur non trouvé"));

        // Retourner les réservations CONFIRMED dans le futur uniquement
        List<Reservation> confirmed = reservationRepository
                .findByCoiffeurIdAndStatusAndStartTimeAfter(
                        coiffeur.getId(),
                        "CONFIRMED",
                        LocalDateTime.now()
                );

        return confirmed.stream()
                .map(r -> new SlotResponse(r.getStartTime(), r.getEndTime()))
                .toList();
    }
}