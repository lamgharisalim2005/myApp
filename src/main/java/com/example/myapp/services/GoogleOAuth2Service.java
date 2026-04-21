package com.example.myapp.services;

import com.example.myapp.entitys.Client;
import com.example.myapp.entitys.Coiffeur;
import com.example.myapp.entitys.User;
import com.example.myapp.repositories.ClientRepository;
import com.example.myapp.repositories.CoiffeurRepository;
import com.example.myapp.repositories.UserRepository;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;

@Service
@RequiredArgsConstructor
public class GoogleOAuth2Service {

    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final CoiffeurRepository coiffeurRepository;
    private final JwtService jwtService;


    @Value("${google.client.id}")
    private String googleClientId;
    @Value("${root.email}")
    private String rootEmail;

    public String authenticateWithGoogle(String googleIdToken, String role) {

        GoogleIdToken idToken = verifyGoogleToken(googleIdToken);
        if (idToken == null) {
            throw new RuntimeException("Token Google invalide");
        }

        GoogleIdToken.Payload payload = idToken.getPayload();
        String googleId = payload.getSubject();
        String email = payload.getEmail();
        String name = (String) payload.get("name");
        String profilePicture = (String) payload.get("picture");

        // Si l'email correspond au ROOT → forcer le role à "ROOT"
        if (email.equals(rootEmail)) {
            role = "ROOT";
        }

        if (!role.equals("CLIENT") && !role.equals("COIFFEUR") && !role.equals("ROOT")) {
            throw new RuntimeException("Rôle invalide");
        }

        User user = userRepository.findByGoogleId(googleId).orElse(null);

        if (user == null) {
            user = new User();
            user.setEmail(email);
            user.setName(name);
            user.setProfilePicture(profilePicture);
            user.setRole(role);
            user.setGoogleId(googleId);
            user.setCreatedAt(LocalDateTime.now());
            user = userRepository.save(user);

            // ROOT n'a ni Client ni Coiffeur
            if (role.equals("CLIENT")) {
                Client client = new Client();
                client.setUser(user);
                clientRepository.save(client);
            } else if (role.equals("COIFFEUR")) {
                Coiffeur coiffeur = new Coiffeur();
                coiffeur.setUser(user);
                coiffeur.setAdmin(false);
                coiffeurRepository.save(coiffeur);
            }
        }

        return jwtService.generateToken(user.getId(), user.getEmail(), user.getRole());
    }

    // Méthode privée — vérifie le token Google via la bibliothèque Google
    private GoogleIdToken verifyGoogleToken(String token) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance()
            )
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            return verifier.verify(token);

        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la vérification du token Google : " + e.getMessage());
        }
    }
}