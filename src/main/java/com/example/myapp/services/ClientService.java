package com.example.myapp.services;

import com.example.myapp.dtos.*;
import com.example.myapp.entitys.*;
import com.example.myapp.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClientService {
    private final ClientRepository clientRepository;
    private final SalonRepository salonRepository;
    private final CoiffeurRepository coiffeurRepository;
    private final ServiceRepository serviceRepository;
    private final CloudinaryService cloudinaryService;
    private final SalonPhotoRepository salonPhotoRepository;
    private final CoiffeurPhotoRepository coiffeurPhotoRepository;

    // PUBLIC — Voir tous les salons
    public List<SalonResponse> getAllSalons() {
        return salonRepository.findAll()
                .stream()
                .map(salon -> new SalonResponse(
                        salon.getId(),
                        salon.getName(),
                        salon.getLocalisation(),
                        salon.getLatitude(),
                        salon.getLongitude()
                ))
                .toList();
    }

    // CLIENT — Voir son profil
    public ProfileResponse getClientProfile(UUID userId) {
        Client client = clientRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Client non trouvé"));

        return new ProfileResponse(
                client.getUser().getId(),           // ← userId
                client.getUser().getName(),
                client.getUser().getEmail(),
                client.getUser().getProfilePicture(),
                client.getUser().getRole(),
                false
        );
    }

    // CLIENT — Modifier son profil
    public ProfileResponse updateClientProfile(UUID userId, UpdateClientRequest request, MultipartFile file) {
        Client client = clientRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Client non trouvé"));

        if (request.name() != null) {
            client.getUser().setName(request.name());
        }

        // Si une nouvelle photo est envoyée → upload vers Cloudinary
        if (file != null && !file.isEmpty()) {
            String url = cloudinaryService.uploadPhoto(file, "profils");
            client.getUser().setProfilePicture(url);
        }

        clientRepository.save(client);

        return new ProfileResponse(
                client.getUser().getId(),           // ← userId
                client.getUser().getName(),
                client.getUser().getEmail(),
                client.getUser().getProfilePicture(),
                client.getUser().getRole(),
                false
        );
    }

    // PUBLIC — Voir les détails d'un salon
    public SalonDetailResponse getSalonDetail(UUID salonId) {
        Salon salon = salonRepository.findById(salonId)
                .orElseThrow(() -> new RuntimeException("Salon non trouvé"));

        List<String> photos = salonPhotoRepository.findBySalonId(salonId)
                .stream()
                .map(SalonPhoto::getUrl)
                .toList();

        List<CoiffeurSalonResponse> coiffeurs = coiffeurRepository.findBySalonId(salonId)
                .stream()
                .map(coiffeur -> new CoiffeurSalonResponse(
                        coiffeur.getUser().getId(),  // ← userId comme valeur de coiffeurId
                        coiffeur.getUser().getName(),
                        coiffeur.getUser().getProfilePicture(),
                        coiffeur.isAdmin()
                ))
                .toList();

        return new SalonDetailResponse(
                salon.getId(),
                salon.getName(),
                salon.getLocalisation(),
                salon.getLatitude(),
                salon.getLongitude(),
                photos,
                coiffeurs
        );
    }

    public CoiffeurDetailResponse getCoiffeurDetail(UUID userId) {
        // ✅ Chercher par userId au lieu de coiffeurId
        Coiffeur coiffeur = coiffeurRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Coiffeur non trouvé"));

        List<String> photos = coiffeurPhotoRepository.findByCoiffeurId(coiffeur.getId())
                .stream()
                .map(CoiffeurPhoto::getUrl)
                .toList();

        List<ServiceResponse> services = serviceRepository.findByCoiffeurId(coiffeur.getId())
                .stream()
                .map(service -> new ServiceResponse(
                        service.getId(),
                        service.getName(),
                        service.getDescription(),
                        service.getPrice(),
                        service.getDuration()
                ))
                .toList();

        SalonResponse salon = null;
        if (coiffeur.getSalon() != null) {
            salon = new SalonResponse(
                    coiffeur.getSalon().getId(),
                    coiffeur.getSalon().getName(),
                    coiffeur.getSalon().getLocalisation(),
                    coiffeur.getSalon().getLatitude(),
                    coiffeur.getSalon().getLongitude()
            );
        }

        return new CoiffeurDetailResponse(
                coiffeur.getUser().getId(),
                coiffeur.getUser().getName(),
                coiffeur.getUser().getEmail(),
                coiffeur.getUser().getProfilePicture(),
                photos,
                services,
                salon
        );
    }

    public ProfileResponse getClientPublicProfile(UUID userId) {
        Client client = clientRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Client non trouvé"));
        return new ProfileResponse(
                client.getUser().getId(),
                client.getUser().getName(),
                client.getUser().getEmail(),
                client.getUser().getProfilePicture(),
                "CLIENT",
                false
        );
    }
}