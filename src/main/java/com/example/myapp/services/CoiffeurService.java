package com.example.myapp.services;

import com.example.myapp.dtos.*;
import com.example.myapp.entitys.*;
import com.example.myapp.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CoiffeurService {
    private final SalonRepository salonRepository;
    private final CoiffeurRepository coiffeurRepository;
    private final SalonRequestRepository salonRequestRepository;
    private final NotificationService notificationService;
    private final ServiceRepository serviceRepository;
    private final WorkScheduleRepository workScheduleRepository;
    private final CloudinaryService cloudinaryService;
    private final CoiffeurPhotoRepository coiffeurPhotoRepository;
    private final SalonPhotoRepository salonPhotoRepository;

    // COIFFEUR — Créer un salon
    public SalonResponse creerSalon(CreateSalonRequest createSalonRequest, UUID userId) {
        Coiffeur coiffeur = coiffeurRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Coiffeur non trouvé"));

        if (coiffeur.getSalon() != null) {
            if (coiffeur.isAdmin()) {
                throw new RuntimeException("Ce coiffeur gère déjà un salon");
            } else {
                throw new RuntimeException("Ce coiffeur est déjà intégré au salon.");
            }
        }

        Salon salon = new Salon();
        salon.setName(createSalonRequest.name());
        salon.setLocalisation(createSalonRequest.localisation());
        salon.setLatitude(createSalonRequest.latitude());
        salon.setLongitude(createSalonRequest.longitude());
        salon.setCreatedAt(LocalDateTime.now());
        salon.setCoiffeur(coiffeur);
        Salon saved = salonRepository.save(salon);

        coiffeur.setAdmin(true);
        coiffeur.setSalon(saved);
        coiffeurRepository.save(coiffeur);

        return new SalonResponse(
                saved.getId(),
                saved.getName(),
                saved.getLocalisation(),
                saved.getLatitude(),
                saved.getLongitude()
        );
    }

    // COIFFEUR — Envoyer une demande pour rejoindre un salon
    public SalonRequestResponse envoyerDemande(JoinSalonRequest request, UUID userId) {
        Coiffeur coiffeur = coiffeurRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Coiffeur non trouvé"));

        if (coiffeur.isAdmin()) {
            throw new RuntimeException("Vous gérez déjà un salon, vous ne pouvez pas envoyer une demande");
        }

        if (salonRequestRepository.existsByCoiffeurIdAndSalonIdAndStatus(
                coiffeur.getId(), request.salonId(), "PENDING")) {
            throw new RuntimeException("Vous avez déjà une demande en cours pour ce salon");
        }

        Salon salon = salonRepository.findById(request.salonId())
                .orElseThrow(() -> new RuntimeException("Salon non trouvé"));

        SalonRequest salonRequest = new SalonRequest();
        salonRequest.setCoiffeur(coiffeur);
        salonRequest.setSalon(salon);
        salonRequest.setStatus("PENDING");
        salonRequest.setCreatedAt(LocalDateTime.now());
        SalonRequest saved = salonRequestRepository.save(salonRequest);

        Coiffeur admin = coiffeurRepository.findBySalonIdAndIsAdminTrue(request.salonId())
                .orElseThrow(() -> new RuntimeException("Admin du salon non trouvé"));

        notificationService.envoyerNotification(
                admin.getUser().getId(),           // ← userId
                "Nouvelle demande",
                "Le coiffeur " + coiffeur.getUser().getName() + " veut rejoindre votre salon",
                saved.getId(),
                "SALON_REQUEST"
        );

        return new SalonRequestResponse(
                saved.getId(),
                saved.getStatus(),
                coiffeur.getUser().getName(),
                salon.getName()
        );
    }

    // ADMIN — Voir les demandes reçues par son salon
    public List<SalonRequestResponse> getDemandesBySalon(UUID salonId, UUID userId) {
        Coiffeur admin = coiffeurRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Coiffeur non trouvé"));

        if (!admin.isAdmin()) {
            throw new RuntimeException("Vous n'êtes pas admin d'un salon");
        }

        if (!admin.getSalon().getId().equals(salonId)) {
            throw new RuntimeException("Ce salon ne vous appartient pas");
        }

        return salonRequestRepository.findBySalonId(salonId)
                .stream()
                .map(demande -> new SalonRequestResponse(
                        demande.getId(),
                        demande.getStatus(),
                        demande.getCoiffeur().getUser().getName(),
                        demande.getSalon().getName()
                ))
                .toList();
    }

    // ADMIN — Accepter ou refuser une demande
    public SalonRequestResponse traiterDemande(UUID demandeId, String status, UUID userId) {
        SalonRequest demande = salonRequestRepository.findById(demandeId)
                .orElseThrow(() -> new RuntimeException("Demande non trouvée"));

        Coiffeur admin = coiffeurRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Coiffeur non trouvé"));

        if (!admin.isAdmin()) {
            throw new RuntimeException("Vous n'êtes pas admin d'un salon");
        }

        if (!admin.getSalon().getId().equals(demande.getSalon().getId())) {
            throw new RuntimeException("Vous n'êtes pas l'admin de ce salon");
        }

        if (!status.equals("ACCEPTED") && !status.equals("REJECTED")) {
            throw new RuntimeException("Statut invalide, utilisez ACCEPTED ou REJECTED");
        }

        if (!demande.getStatus().equals("PENDING")) {
            throw new RuntimeException("Cette demande a déjà été traitée — décision : " + demande.getStatus());
        }

        demande.setStatus(status);
        salonRequestRepository.save(demande);

        if (status.equals("ACCEPTED")) {
            Coiffeur coiffeur = demande.getCoiffeur();

            if (coiffeur.getSalon() != null) {
                coiffeur.setSalon(null);
                coiffeur.setAdmin(false);
            }

            coiffeur.setSalon(demande.getSalon());
            coiffeurRepository.save(coiffeur);

            notificationService.envoyerNotification(
                    coiffeur.getUser().getId(),
                    "Demande acceptée",
                    "Votre demande pour rejoindre le salon " +
                            demande.getSalon().getName() + " a été acceptée",
                    demande.getId(),
                    "SALON_REQUEST"
            );
        }

        if (status.equals("REJECTED")) {
            notificationService.envoyerNotification(
                    demande.getCoiffeur().getUser().getId(), // ← userId
                    "Demande refusée",
                    "Votre demande pour rejoindre le salon " +
                            demande.getSalon().getName() + " a été refusée",
                    demande.getId(),
                    "SALON_REQUEST"
            );
        }

        return new SalonRequestResponse(
                demande.getId(),
                demande.getStatus(),
                demande.getCoiffeur().getUser().getName(),
                demande.getSalon().getName()
        );
    }

    // COIFFEUR — Créer un service
    public ServiceResponse creerService(CreateServiceRequest request, UUID userId) {
        Coiffeur coiffeur = coiffeurRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Coiffeur non trouvé"));

        com.example.myapp.entitys.Service service = new com.example.myapp.entitys.Service();
        service.setCoiffeur(coiffeur);
        service.setName(request.name());
        service.setDescription(request.description());
        service.setPrice(request.price());
        service.setDuration(request.duration());
        com.example.myapp.entitys.Service saved = serviceRepository.save(service);

        return new ServiceResponse(
                saved.getId(),
                saved.getName(),
                saved.getDescription(),
                saved.getPrice(),
                saved.getDuration()
        );
    }

    // COIFFEUR — Modifier un service
    public ServiceResponse modifierService(UUID id, UpdateServiceRequest request, UUID userId) {
        com.example.myapp.entitys.Service service = serviceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Service non trouvé"));

        Coiffeur coiffeur = coiffeurRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Coiffeur non trouvé"));

        if (!service.getCoiffeur().getId().equals(coiffeur.getId())) {
            throw new RuntimeException("Ce service ne vous appartient pas");
        }

        service.setName(request.name());
        service.setDescription(request.description());
        service.setPrice(request.price());
        service.setDuration(request.duration());
        com.example.myapp.entitys.Service saved = serviceRepository.save(service);

        return new ServiceResponse(
                saved.getId(),
                saved.getName(),
                saved.getDescription(),
                saved.getPrice(),
                saved.getDuration()
        );
    }

    // COIFFEUR — Supprimer un service
    public void supprimerService(UUID id, UUID userId) {
        com.example.myapp.entitys.Service service = serviceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Service non trouvé"));

        Coiffeur coiffeur = coiffeurRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Coiffeur non trouvé"));

        if (!service.getCoiffeur().getId().equals(coiffeur.getId())) {
            throw new RuntimeException("Ce service ne vous appartient pas");
        }

        serviceRepository.delete(service);
    }

    // COIFFEUR — Créer un horaire
    public WorkScheduleResponse creerWorkSchedule(CreateWorkScheduleRequest request, UUID userId) {
        Coiffeur coiffeur = coiffeurRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Coiffeur non trouvé"));

        List<WorkSchedule> existants = workScheduleRepository
                .findByCoiffeurIdAndDayOfWeek(coiffeur.getId(), request.dayOfWeek());

        for (WorkSchedule ws : existants) {
            if (request.startTime().isBefore(ws.getEndTime()) &&
                    request.endTime().isAfter(ws.getStartTime())) {
                throw new RuntimeException("Ce créneau chevauche un créneau existant");
            }
        }

        WorkSchedule workSchedule = new WorkSchedule();
        workSchedule.setCoiffeur(coiffeur);
        workSchedule.setDayOfWeek(request.dayOfWeek());
        workSchedule.setStartTime(request.startTime());
        workSchedule.setEndTime(request.endTime());
        WorkSchedule saved = workScheduleRepository.save(workSchedule);

        return new WorkScheduleResponse(
                saved.getId(),
                saved.getDayOfWeek(),
                saved.getStartTime(),
                saved.getEndTime(),
                coiffeur.getUser().getId()  // ← userId
        );
    }

    // COIFFEUR — Modifier un horaire
    public WorkScheduleResponse modifierWorkSchedule(UUID id, UpdateWorkScheduleRequest request, UUID userId) {
        WorkSchedule workSchedule = workScheduleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("WorkSchedule non trouvé"));

        Coiffeur coiffeur = coiffeurRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Coiffeur non trouvé"));

        if (!workSchedule.getCoiffeur().getId().equals(coiffeur.getId())) {
            throw new RuntimeException("Ce WorkSchedule ne vous appartient pas");
        }

        List<WorkSchedule> existants = workScheduleRepository
                .findByCoiffeurIdAndDayOfWeek(coiffeur.getId(), request.dayOfWeek());

        for (WorkSchedule ws : existants) {
            if (ws.getId().equals(id)) continue;
            if (request.startTime().isBefore(ws.getEndTime()) &&
                    request.endTime().isAfter(ws.getStartTime())) {
                throw new RuntimeException("Ce créneau chevauche un créneau existant");
            }
        }

        workSchedule.setDayOfWeek(request.dayOfWeek());
        workSchedule.setStartTime(request.startTime());
        workSchedule.setEndTime(request.endTime());
        WorkSchedule saved = workScheduleRepository.save(workSchedule);

        return new WorkScheduleResponse(
                saved.getId(),
                saved.getDayOfWeek(),
                saved.getStartTime(),
                saved.getEndTime(),
                saved.getCoiffeur().getUser().getId()  // ← userId
        );
    }

    // COIFFEUR — Supprimer un horaire
    public void supprimerWorkSchedule(UUID id, UUID userId) {
        WorkSchedule workSchedule = workScheduleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("WorkSchedule non trouvé"));

        Coiffeur coiffeur = coiffeurRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Coiffeur non trouvé"));

        if (!workSchedule.getCoiffeur().getId().equals(coiffeur.getId())) {
            throw new RuntimeException("Ce WorkSchedule ne vous appartient pas");
        }

        workScheduleRepository.delete(workSchedule);
    }

    // PUBLIC — Voir les horaires d'un coiffeur
    // ✅ Après
    public List<WorkScheduleResponse> getWorkSchedules(UUID userId) {
        Coiffeur coiffeur = coiffeurRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Coiffeur non trouvé"));

        return workScheduleRepository.findByCoiffeurId(coiffeur.getId())
                .stream()
                .map(ws -> new WorkScheduleResponse(
                        ws.getId(),
                        ws.getDayOfWeek(),
                        ws.getStartTime(),
                        ws.getEndTime(),
                        ws.getCoiffeur().getUser().getId()
                ))
                .toList();
    }

    // COIFFEUR — Voir son profil
    public ProfileResponse getCoiffeurProfile(UUID userId) {
        Coiffeur coiffeur = coiffeurRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Coiffeur non trouvé"));

        return new ProfileResponse(
                coiffeur.getUser().getId(),          // ← userId
                coiffeur.getUser().getName(),
                coiffeur.getUser().getEmail(),
                coiffeur.getUser().getProfilePicture(),
                coiffeur.getUser().getRole()
        );
    }

    // COIFFEUR — Modifier son profil
    public ProfileResponse updateCoiffeurProfile(UUID userId, UpdateCoiffeurRequest request, MultipartFile file) {
        Coiffeur coiffeur = coiffeurRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Coiffeur non trouvé"));

        if (request.name() != null) {
            coiffeur.getUser().setName(request.name());
        }

        if (file != null && !file.isEmpty()) {
            String url = cloudinaryService.uploadPhoto(file, "profils");
            coiffeur.getUser().setProfilePicture(url);
        }

        coiffeurRepository.save(coiffeur);

        return new ProfileResponse(
                coiffeur.getUser().getId(),          // ← userId
                coiffeur.getUser().getName(),
                coiffeur.getUser().getEmail(),
                coiffeur.getUser().getProfilePicture(),
                coiffeur.getUser().getRole()
        );
    }

    // COIFFEUR — Ajouter une photo
    public PhotoResponse ajouterPhotoCoiffeur(UUID userId, MultipartFile file) {
        Coiffeur coiffeur = coiffeurRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Coiffeur non trouvé"));

        String url = cloudinaryService.uploadPhoto(file, "coiffeurs");

        CoiffeurPhoto photo = new CoiffeurPhoto();
        photo.setCoiffeur(coiffeur);
        photo.setUrl(url);
        photo.setCreatedAt(LocalDateTime.now());
        CoiffeurPhoto saved = coiffeurPhotoRepository.save(photo);

        return new PhotoResponse(saved.getId(), saved.getUrl());
    }

    // PUBLIC — Voir les photos d'un coiffeur
    // ✅ Après
    public List<PhotoResponse> getPhotosCoiffeur(UUID userId) {
        Coiffeur coiffeur = coiffeurRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Coiffeur non trouvé"));

        return coiffeurPhotoRepository.findByCoiffeurId(coiffeur.getId())
                .stream()
                .map(photo -> new PhotoResponse(photo.getId(), photo.getUrl()))
                .toList();
    }

    // COIFFEUR — Supprimer une photo
    public void supprimerPhotoCoiffeur(UUID photoId, UUID userId) {
        CoiffeurPhoto photo = coiffeurPhotoRepository.findById(photoId)
                .orElseThrow(() -> new RuntimeException("Photo non trouvée"));

        Coiffeur coiffeur = coiffeurRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Coiffeur non trouvé"));

        if (!photo.getCoiffeur().getId().equals(coiffeur.getId())) {
            throw new RuntimeException("Cette photo ne vous appartient pas");
        }

        coiffeurPhotoRepository.delete(photo);
    }

    // ADMIN — Ajouter une photo au salon
    public PhotoResponse ajouterPhotoSalon(UUID salonId, MultipartFile file, UUID userId) {
        Salon salon = salonRepository.findById(salonId)
                .orElseThrow(() -> new RuntimeException("Salon non trouvé"));

        Coiffeur coiffeur = coiffeurRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Coiffeur non trouvé"));

        if (!coiffeur.isAdmin()) {
            throw new RuntimeException("Vous n'êtes pas admin d'un salon");
        }

        if (!coiffeur.getSalon().getId().equals(salonId)) {
            throw new RuntimeException("Ce salon ne vous appartient pas");
        }

        String url = cloudinaryService.uploadPhoto(file, "salons");

        SalonPhoto photo = new SalonPhoto();
        photo.setSalon(salon);
        photo.setUrl(url);
        photo.setCreatedAt(LocalDateTime.now());
        SalonPhoto saved = salonPhotoRepository.save(photo);

        return new PhotoResponse(saved.getId(), saved.getUrl());
    }

    // PUBLIC — Voir les photos d'un salon
    public List<PhotoResponse> getPhotosSalon(UUID salonId) {
        salonRepository.findById(salonId)
                .orElseThrow(() -> new RuntimeException("Salon non trouvé"));

        return salonPhotoRepository.findBySalonId(salonId)
                .stream()
                .map(photo -> new PhotoResponse(photo.getId(), photo.getUrl()))
                .toList();
    }

    // ADMIN — Supprimer une photo du salon
    public void supprimerPhotoSalon(UUID photoId, UUID userId) {
        SalonPhoto photo = salonPhotoRepository.findById(photoId)
                .orElseThrow(() -> new RuntimeException("Photo non trouvée"));

        Coiffeur coiffeur = coiffeurRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Coiffeur non trouvé"));

        if (!coiffeur.isAdmin()) {
            throw new RuntimeException("Vous n'êtes pas admin d'un salon");
        }

        if (!coiffeur.getSalon().getId().equals(photo.getSalon().getId())) {
            throw new RuntimeException("Cette photo n'appartient pas à votre salon");
        }

        salonPhotoRepository.delete(photo);
    }

    // ADMIN — Retirer un coiffeur du salon
    public void retirerCoiffeur(UUID salonId, UUID coiffeurIdARetirer, UUID userId) {
        Coiffeur admin = coiffeurRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Coiffeur non trouvé"));

        if (!admin.isAdmin()) {
            throw new RuntimeException("Vous n'êtes pas admin d'un salon");
        }

        if (salonRepository.findById(salonId).isEmpty())
            throw new RuntimeException("Salon non trouvé");

        if (!admin.getSalon().getId().equals(salonId)) {
            throw new RuntimeException("Ce salon ne vous appartient pas");
        }

        // Frontend envoie coiffeurId (qui est userId) → on cherche par userId
        Coiffeur coiffeur = coiffeurRepository.findByUserId(coiffeurIdARetirer)
                .orElseThrow(() -> new RuntimeException("Coiffeur non trouvé"));

        if (coiffeur.getSalon() == null ||
                !coiffeur.getSalon().getId().equals(admin.getSalon().getId())) {
            throw new RuntimeException("Ce coiffeur ne travaille pas dans votre salon");
        }

        if (admin.getId().equals(coiffeur.getId())) {
            throw new RuntimeException("Vous ne pouvez pas vous retirer vous-même");
        }

        if (coiffeur.isAdmin()) {
            throw new RuntimeException("Vous ne pouvez pas retirer un admin");
        }

        String nomSalon = admin.getSalon().getName();
        coiffeur.setSalon(null);
        coiffeur.setAdmin(false);
        coiffeurRepository.save(coiffeur);

        notificationService.envoyerNotification(
                coiffeur.getUser().getId(),          // ← userId
                "Retiré du salon",
                "Vous avez été retiré du salon " + nomSalon,
                admin.getSalon().getId(),
                "SALON"
        );
    }

    // ADMIN — Transférer la propriété du salon
    public void transfererPropriete(UUID salonId, UUID nouveauAdminId, UUID userId) {
        Coiffeur admin = coiffeurRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Coiffeur non trouvé"));

        if (!admin.isAdmin()) {
            throw new RuntimeException("Vous n'êtes pas admin d'un salon");
        }

        if (salonRepository.findById(salonId).isEmpty())
            throw new RuntimeException("Salon non trouvé");

        if (!admin.getSalon().getId().equals(salonId)) {
            throw new RuntimeException("Ce salon ne vous appartient pas");
        }

        // Frontend envoie nouveauAdminId (qui est userId) → on cherche par userId
        Coiffeur nouveauAdmin = coiffeurRepository.findByUserId(nouveauAdminId)
                .orElseThrow(() -> new RuntimeException("Coiffeur non trouvé"));

        if (nouveauAdmin.getSalon() == null ||
                !nouveauAdmin.getSalon().getId().equals(salonId)) {
            throw new RuntimeException("Ce coiffeur ne travaille pas dans votre salon");
        }

        if (admin.getId().equals(nouveauAdmin.getId())) {
            throw new RuntimeException("Vous ne pouvez pas vous transférer la propriété à vous-même");
        }

        admin.setAdmin(false);
        coiffeurRepository.save(admin);

        nouveauAdmin.setAdmin(true);
        coiffeurRepository.save(nouveauAdmin);

        Salon salon = admin.getSalon();
        salon.setCoiffeur(nouveauAdmin);
        salonRepository.save(salon);

        notificationService.envoyerNotification(
                nouveauAdmin.getUser().getId(),      // ← userId

                "Nouveau admin",
                "Vous êtes maintenant admin du salon " + salon.getName(),
                salon.getId(),
                "SALON"
        );

        notificationService.envoyerNotification(
                admin.getUser().getId(),             // ← userId
                "Propriété transférée",
                "Vous avez transféré la propriété du salon " + salon.getName() + " à " + nouveauAdmin.getUser().getName(),
                salon.getId(),
                "SALON"
        );
    }

    // ADMIN — Supprimer le salon
    public void supprimerSalon(UUID salonId, UUID userId) {
        Coiffeur admin = coiffeurRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Coiffeur non trouvé"));

        if (!admin.isAdmin()) {
            throw new RuntimeException("Vous n'êtes pas admin d'un salon");
        }
        if (salonRepository.findById(salonId).isEmpty())
            throw new RuntimeException("Salon non trouvé");

        if (!admin.getSalon().getId().equals(salonId)) {
            throw new RuntimeException("Ce salon ne vous appartient pas");
        }

        Salon salon = salonRepository.findById(salonId)
                .orElseThrow(() -> new RuntimeException("Salon non trouvé"));

        List<Coiffeur> coiffeurs = coiffeurRepository.findBySalonId(salonId);

        coiffeurs.forEach(coiffeur -> {
            coiffeur.setSalon(null);
            coiffeur.setAdmin(false);
            coiffeurRepository.save(coiffeur);

            notificationService.envoyerNotification(
                    coiffeur.getUser().getId(),      // ← userId
                    "Salon supprimé",
                    "Le salon " + salon.getName() + " a été supprimé",
                    salonId,
                    "SALON"
            );
        });

        salonRepository.delete(salon);
    }

    // COIFFEUR — Quitter le salon
    public void quitterSalon(UUID userId) {
        Coiffeur coiffeur = coiffeurRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Coiffeur non trouvé"));

        if (coiffeur.getSalon() == null) {
            throw new RuntimeException("Vous n'appartenez à aucun salon");
        }

        if (coiffeur.isAdmin()) {
            throw new RuntimeException("Vous êtes admin — transférez la propriété ou supprimez le salon avant de quitter");
        }

        Salon salon = coiffeur.getSalon();

        Coiffeur admin = coiffeurRepository.findBySalonIdAndIsAdminTrue(salon.getId())
                .orElse(null);

        coiffeur.setSalon(null);
        coiffeurRepository.save(coiffeur);

        if (admin != null) {
            notificationService.envoyerNotification(
                    admin.getUser().getId(),         // ← userId
                    "Coiffeur parti",
                    coiffeur.getUser().getName() + " a quitté votre salon",
                    salon.getId(),
                    "SALON"
            );
        }
    }

    // COIFFEUR — Voir ses demandes envoyées
    public List<SalonRequestResponse> getDemandesByCoiffeur(UUID userId) {
        Coiffeur coiffeur = coiffeurRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Coiffeur non trouvé"));

        return salonRequestRepository.findByCoiffeurId(coiffeur.getId())
                .stream()
                .map(demande -> new SalonRequestResponse(
                        demande.getId(),
                        demande.getStatus(),
                        demande.getCoiffeur().getUser().getName(),
                        demande.getSalon().getName()
                ))
                .toList();
    }
}