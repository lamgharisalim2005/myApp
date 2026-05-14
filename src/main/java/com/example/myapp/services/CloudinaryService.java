package com.example.myapp.services;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
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
        try {
            // Extraire le public_id depuis l'URL
            String publicId = url
                    .replaceAll("https://res.cloudinary.com/[^/]+/image/upload/v[0-9]+/", "")
                    .replaceAll("\\.[^.]+$", ""); // supprimer l'extension

            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());

        } catch (IOException e) {
            throw new RuntimeException("Erreur suppression Cloudinary");
        }
    }


}