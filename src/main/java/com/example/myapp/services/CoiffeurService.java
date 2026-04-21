package com.example.myapp.services;

import com.example.myapp.dtos.*;
import com.example.myapp.entitys.*;
import com.example.myapp.repositories.*;
import lombok.RequiredArgsConstructor;
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

    public SalonResponse creerSalon(CreateSalonRequest createSalonRequest) {
        Coiffeur coiffeur = coiffeurRepository.findById(createSalonRequest.coiffeurId())
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

    public SalonRequestResponse envoyerDemande(JoinSalonRequest request) {
        Coiffeur coiffeur = coiffeurRepository.findById(request.coiffeurId())
                .orElseThrow(() -> new RuntimeException("Coiffeur non trouvé"));

        if (coiffeur.isAdmin()) {
            throw new RuntimeException("Vous gérez déjà un salon, vous ne pouvez pas envoyer une demande");
        }

        if (salonRequestRepository.existsByCoiffeurIdAndSalonIdAndStatus(
                request.coiffeurId(), request.salonId(), "PENDING")) {
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
                admin.getId(),
                "COIFFEUR",
                "Nouvelle demande",
                "Le coiffeur " + coiffeur.getUser().getName() + " veut rejoindre votre salon", // ← via User
                saved.getId(),
                "SALON_REQUEST"
        );

        return new SalonRequestResponse(
                saved.getId(),
                saved.getStatus(),
                coiffeur.getUser().getName(), // ← via User
                salon.getName()
        );
    }

    public List<SalonRequestResponse> getDemandesBySalon(UUID salonId, UUID adminId) {
        Coiffeur admin = coiffeurRepository.findById(adminId)
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
                        demande.getCoiffeur().getUser().getName(), // ← via User
                        demande.getSalon().getName()
                ))
                .toList();
    }

    public SalonRequestResponse traiterDemande(UUID demandeId, String status, UUID adminId) {
        SalonRequest demande = salonRequestRepository.findById(demandeId)
                .orElseThrow(() -> new RuntimeException("Demande non trouvée"));

        Coiffeur admin = coiffeurRepository.findById(adminId)
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
                    coiffeur.getId(),
                    "COIFFEUR",
                    "Demande acceptée",
                    "Votre demande pour rejoindre le salon " +
                            demande.getSalon().getName() + " a été acceptée",
                    demande.getId(),
                    "SALON_REQUEST"
            );
        }

        if (status.equals("REJECTED")) {
            notificationService.envoyerNotification(
                    demande.getCoiffeur().getId(),
                    "COIFFEUR",
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
                demande.getCoiffeur().getUser().getName(), // ← via User
                demande.getSalon().getName()
        );
    }

    public ServiceResponse creerService(CreateServiceRequest request) {
        Coiffeur coiffeur = coiffeurRepository.findById(request.coiffeurId())
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

    public ServiceResponse modifierService(UUID id, UpdateServiceRequest request, UUID coiffeurId) {
        com.example.myapp.entitys.Service service = serviceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Service non trouvé"));

        if (!service.getCoiffeur().getId().equals(coiffeurId)) {
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

    public void supprimerService(UUID id, UUID coiffeurId) {
        com.example.myapp.entitys.Service service = serviceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Service non trouvé"));

        if (!service.getCoiffeur().getId().equals(coiffeurId)) {
            throw new RuntimeException("Ce service ne vous appartient pas");
        }

        serviceRepository.delete(service);
    }

    public WorkScheduleResponse creerWorkSchedule(CreateWorkScheduleRequest request) {
        Coiffeur coiffeur = coiffeurRepository.findById(request.coiffeurId())
                .orElseThrow(() -> new RuntimeException("Coiffeur non trouvé"));

        List<WorkSchedule> existants = workScheduleRepository
                .findByCoiffeurIdAndDayOfWeek(request.coiffeurId(), request.dayOfWeek());

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
                coiffeur.getId()
        );
    }

    public WorkScheduleResponse modifierWorkSchedule(UUID id, UpdateWorkScheduleRequest request, UUID coiffeurId) {
        WorkSchedule workSchedule = workScheduleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("WorkSchedule non trouvé"));

        if (!workSchedule.getCoiffeur().getId().equals(coiffeurId)) {
            throw new RuntimeException("Ce WorkSchedule ne vous appartient pas");
        }

        List<WorkSchedule> existants = workScheduleRepository
                .findByCoiffeurIdAndDayOfWeek(coiffeurId, request.dayOfWeek());

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
                saved.getCoiffeur().getId()
        );
    }

    public void supprimerWorkSchedule(UUID id, UUID coiffeurId) {
        WorkSchedule workSchedule = workScheduleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("WorkSchedule non trouvé"));

        if (!workSchedule.getCoiffeur().getId().equals(coiffeurId)) {
            throw new RuntimeException("Ce WorkSchedule ne vous appartient pas");
        }

        workScheduleRepository.delete(workSchedule);
    }

    public List<WorkScheduleResponse> getWorkSchedules(UUID coiffeurId) {
        // Vérifier que le coiffeur existe
        coiffeurRepository.findById(coiffeurId)
                .orElseThrow(() -> new RuntimeException("Coiffeur non trouvé"));

        return workScheduleRepository.findByCoiffeurId(coiffeurId)
                .stream()
                .map(ws -> new WorkScheduleResponse(
                        ws.getId(),
                        ws.getDayOfWeek(),
                        ws.getStartTime(),
                        ws.getEndTime(),
                        ws.getCoiffeur().getId()
                ))
                .toList();
    }

    public ProfileResponse getCoiffeurProfile(UUID coiffeurId) {
        Coiffeur coiffeur = coiffeurRepository.findById(coiffeurId)
                .orElseThrow(() -> new RuntimeException("Coiffeur non trouvé"));

        return new ProfileResponse(
                coiffeur.getId(),
                coiffeur.getUser().getName(),          // ← via User
                coiffeur.getUser().getEmail(),         // ← via User
                coiffeur.getUser().getProfilePicture(), // ← via User
                coiffeur.getUser().getRole()           // ← via User
        );
    }

    public ProfileResponse updateCoiffeurProfile(UUID coiffeurId, UpdateCoiffeurRequest request, MultipartFile file) {
        Coiffeur coiffeur = coiffeurRepository.findById(coiffeurId)
                .orElseThrow(() -> new RuntimeException("Coiffeur non trouvé"));

        if (request.name() != null) {
            coiffeur.getUser().setName(request.name()); // ← via User
        }

        if (file != null && !file.isEmpty()) {
            String url = cloudinaryService.uploadPhoto(file, "profils");
            coiffeur.getUser().setProfilePicture(url); // ← via User
        }

        coiffeurRepository.save(coiffeur);

        return new ProfileResponse(
                coiffeur.getId(),
                coiffeur.getUser().getName(),
                coiffeur.getUser().getEmail(),
                coiffeur.getUser().getProfilePicture(),
                coiffeur.getUser().getRole()
        );
    }

    public PhotoResponse ajouterPhotoCoiffeur(UUID coiffeurId, MultipartFile file) {
        Coiffeur coiffeur = coiffeurRepository.findById(coiffeurId)
                .orElseThrow(() -> new RuntimeException("Coiffeur non trouvé"));

        String url = cloudinaryService.uploadPhoto(file, "coiffeurs");

        CoiffeurPhoto photo = new CoiffeurPhoto();
        photo.setCoiffeur(coiffeur);
        photo.setUrl(url);
        photo.setCreatedAt(LocalDateTime.now());
        CoiffeurPhoto saved = coiffeurPhotoRepository.save(photo);

        return new PhotoResponse(saved.getId(), saved.getUrl());
    }

    public List<PhotoResponse> getPhotosCoiffeur(UUID coiffeurId) {
        // Vérifier que le coiffeur existe
        coiffeurRepository.findById(coiffeurId)
                .orElseThrow(() -> new RuntimeException("Coiffeur non trouvé"));

        return coiffeurPhotoRepository.findByCoiffeurId(coiffeurId)
                .stream()
                .map(photo -> new PhotoResponse(photo.getId(), photo.getUrl()))
                .toList();
    }

    public void supprimerPhotoCoiffeur(UUID photoId, UUID coiffeurId) {
        CoiffeurPhoto photo = coiffeurPhotoRepository.findById(photoId)
                .orElseThrow(() -> new RuntimeException("Photo non trouvée"));

        if (!photo.getCoiffeur().getId().equals(coiffeurId)) {
            throw new RuntimeException("Cette photo ne vous appartient pas");
        }

        coiffeurPhotoRepository.delete(photo);
    }

    public PhotoResponse ajouterPhotoSalon(UUID salonId, MultipartFile file, UUID coiffeurId) {
        Salon salon = salonRepository.findById(salonId)
                .orElseThrow(() -> new RuntimeException("Salon non trouvé"));

        Coiffeur coiffeur = coiffeurRepository.findById(coiffeurId)
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

    public List<PhotoResponse> getPhotosSalon(UUID salonId) {
        // Vérifier que le salon existe
        salonRepository.findById(salonId)
                .orElseThrow(() -> new RuntimeException("Salon non trouvé"));

        return salonPhotoRepository.findBySalonId(salonId)
                .stream()
                .map(photo -> new PhotoResponse(photo.getId(), photo.getUrl()))
                .toList();
    }

    public void supprimerPhotoSalon(UUID photoId, UUID coiffeurId) {
        SalonPhoto photo = salonPhotoRepository.findById(photoId)
                .orElseThrow(() -> new RuntimeException("Photo non trouvée"));

        Coiffeur coiffeur = coiffeurRepository.findById(coiffeurId)
                .orElseThrow(() -> new RuntimeException("Coiffeur non trouvé"));

        if (!coiffeur.isAdmin()) {
            throw new RuntimeException("Vous n'êtes pas admin d'un salon");
        }

        if (!coiffeur.getSalon().getId().equals(photo.getSalon().getId())) {
            throw new RuntimeException("Cette photo n'appartient pas à votre salon");
        }

        salonPhotoRepository.delete(photo);
    }

    public void retirerCoiffeur(UUID salonId, RetirerCoiffeurRequest request) {
        Coiffeur admin = coiffeurRepository.findById(request.adminId())
                .orElseThrow(() -> new RuntimeException("Coiffeur non trouvé"));

        if (!admin.isAdmin()) {
            throw new RuntimeException("Vous n'êtes pas admin d'un salon");
        }

        if (!admin.getSalon().getId().equals(salonId)) {
            throw new RuntimeException("Ce salon ne vous appartient pas");
        }

        Coiffeur coiffeur = coiffeurRepository.findById(request.coiffeurId())
                .orElseThrow(() -> new RuntimeException("Coiffeur non trouvé"));

        if (coiffeur.getSalon() == null ||
                !coiffeur.getSalon().getId().equals(admin.getSalon().getId())) {
            throw new RuntimeException("Ce coiffeur ne travaille pas dans votre salon");
        }

        if (request.adminId().equals(request.coiffeurId())) {
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
                coiffeur.getId(),
                "COIFFEUR",
                "Retiré du salon",
                "Vous avez été retiré du salon " + nomSalon,
                admin.getSalon().getId(),
                "SALON"
        );
    }

    public void transfererPropriete(UUID salonId, TransfererProprieteRequest request) {
        Coiffeur admin = coiffeurRepository.findById(request.adminId())
                .orElseThrow(() -> new RuntimeException("Coiffeur non trouvé"));

        if (!admin.isAdmin()) {
            throw new RuntimeException("Vous n'êtes pas admin d'un salon");
        }

        if (!admin.getSalon().getId().equals(salonId)) {
            throw new RuntimeException("Ce salon ne vous appartient pas");
        }

        Coiffeur nouveauAdmin = coiffeurRepository.findById(request.nouveauAdminId())
                .orElseThrow(() -> new RuntimeException("Coiffeur non trouvé"));

        if (nouveauAdmin.getSalon() == null ||
                !nouveauAdmin.getSalon().getId().equals(salonId)) {
            throw new RuntimeException("Ce coiffeur ne travaille pas dans votre salon");
        }

        if (request.adminId().equals(request.nouveauAdminId())) {
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
                nouveauAdmin.getId(),
                "COIFFEUR",
                "Nouveau admin",
                "Vous êtes maintenant admin du salon " + salon.getName(),
                salon.getId(),
                "SALON"
        );

        notificationService.envoyerNotification(
                admin.getId(),
                "COIFFEUR",
                "Propriété transférée",
                "Vous avez transféré la propriété du salon " + salon.getName() + " à " + nouveauAdmin.getUser().getName(), // ← via User
                salon.getId(),
                "SALON"
        );
    }

    public void supprimerSalon(UUID salonId, UUID adminId) {
        Coiffeur admin = coiffeurRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Coiffeur non trouvé"));

        if (!admin.isAdmin()) {
            throw new RuntimeException("Vous n'êtes pas admin d'un salon");
        }

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
                    coiffeur.getId(),
                    "COIFFEUR",
                    "Salon supprimé",
                    "Le salon " + salon.getName() + " a été supprimé",
                    salonId,
                    "SALON"
            );
        });

        salonRepository.delete(salon);
    }

    public void quitterSalon(QuitterSalonRequest request) {
        Coiffeur coiffeur = coiffeurRepository.findById(request.coiffeurId())
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
                    admin.getId(),
                    "COIFFEUR",
                    "Coiffeur parti",
                    coiffeur.getUser().getName() + " a quitté votre salon", // ← via User
                    salon.getId(),
                    "SALON"
            );
        }
    }

    public List<SalonRequestResponse> getDemandesByCoiffeur(UUID coiffeurId) {
        coiffeurRepository.findById(coiffeurId)
                .orElseThrow(() -> new RuntimeException("Coiffeur non trouvé"));

        return salonRequestRepository.findByCoiffeurId(coiffeurId)
                .stream()
                .map(demande -> new SalonRequestResponse(
                        demande.getId(),
                        demande.getStatus(),
                        demande.getCoiffeur().getUser().getName(), // ← via User
                        demande.getSalon().getName()
                ))
                .toList();
    }
}