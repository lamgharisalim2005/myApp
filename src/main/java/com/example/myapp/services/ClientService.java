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

    public ProfileResponse getClientProfile(UUID clientId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client non trouvé"));

        // ← Accès aux infos via User maintenant
        return new ProfileResponse(
                client.getId(),
                client.getUser().getName(),
                client.getUser().getEmail(),
                client.getUser().getProfilePicture(),
                client.getUser().getRole()
        );
    }

    public ProfileResponse updateClientProfile(UUID clientId, UpdateClientRequest request, MultipartFile file) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client non trouvé"));

        // ← Modification via User maintenant
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
                client.getId(),
                client.getUser().getName(),
                client.getUser().getEmail(),
                client.getUser().getProfilePicture(),
                client.getUser().getRole()
        );
    }

    public SalonDetailResponse getSalonDetail(UUID salonId) {
        // 1. Vérifier que le salon existe
        Salon salon = salonRepository.findById(salonId)
                .orElseThrow(() -> new RuntimeException("Salon non trouvé"));

        // 2. Récupérer les photos du salon
        List<String> photos = salonPhotoRepository.findBySalonId(salonId)
                .stream()
                .map(SalonPhoto::getUrl)
                .toList();

        // 3. Récupérer les coiffeurs du salon
        List<CoiffeurSalonResponse> coiffeurs = coiffeurRepository.findBySalonId(salonId)
                .stream()
                .map(coiffeur -> new CoiffeurSalonResponse(
                        coiffeur.getId(),
                        coiffeur.getUser().getName(),          // ← via User
                        coiffeur.getUser().getProfilePicture(), // ← via User
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

    public CoiffeurDetailResponse getCoiffeurDetail(UUID coiffeurId) {
        Coiffeur coiffeur = coiffeurRepository.findById(coiffeurId)
                .orElseThrow(() -> new RuntimeException("Coiffeur non trouvé"));

        List<String> photos = coiffeurPhotoRepository.findByCoiffeurId(coiffeurId)
                .stream()
                .map(CoiffeurPhoto::getUrl)
                .toList();

        List<ServiceResponse> services = serviceRepository.findByCoiffeurId(coiffeurId)
                .stream()
                .map(service -> new ServiceResponse(
                        service.getId(),
                        service.getName(),
                        service.getDescription(),
                        service.getPrice(),
                        service.getDuration()
                ))
                .toList();

        // Récupérer le salon du coiffeur s'il existe
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
                coiffeur.getId(),
                coiffeur.getUser().getName(),          // ← via User
                coiffeur.getUser().getEmail(),         // ← via User
                coiffeur.getUser().getProfilePicture(), // ← via User
                photos,
                services,
                salon
        );
    }
}