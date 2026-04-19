package com.example.myapp.services;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public String uploadPhoto(MultipartFile file, String dossier) {
        try {
            Map result = cloudinary.uploader().unsignedUpload(
                    file.getBytes(),
                    "coiffeur_preset", // nom du preset
                    ObjectUtils.asMap(
                            "folder", dossier
                    )
            );
            return result.get("secure_url").toString();

        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de l'upload de la photo");
        }
    }

    public void supprimerPhoto(String url) {
        // TODO: Supprimer de Cloudinary quand le problème de timestamp sera résolu
        // Pour l'instant on supprime juste de la base de données
    }
}