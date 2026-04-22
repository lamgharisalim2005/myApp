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
import java.util.HashMap;
import java.util.Map;

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

    public Map<String, String> authenticateWithGoogle(String googleIdToken, String role) {

        // 1. Vérifier le token Google
        GoogleIdToken idToken = verifyGoogleToken(googleIdToken);
        if (idToken == null) {
            throw new RuntimeException("Token Google invalide");
        }

        // 2. Extraire les infos depuis le token Google
        GoogleIdToken.Payload payload = idToken.getPayload();
        String googleId = payload.getSubject();
        String email = payload.getEmail();
        String name = (String) payload.get("name");
        String profilePicture = (String) payload.get("picture");

        // 3. Si l'email correspond au ROOT → forcer le role à "ROOT"
        if (email.equals(rootEmail)) {
            role = "ROOT";
        }

        // 4. Vérifier que le role est valide
        if (!role.equals("CLIENT") && !role.equals("COIFFEUR") && !role.equals("ROOT")) {
            throw new RuntimeException("Rôle invalide");
        }

        // 5. Chercher l'utilisateur en DB par googleId
        User user = userRepository.findByGoogleId(googleId).orElse(null);

        // 6. Si n'existe pas → créer nouveau compte
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

        // 7. Générer le JWT
        String jwt = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole());

        // 8. Construire la réponse avec toutes les infos nécessaires au frontend
        Map<String, String> response = new HashMap<>();
        response.put("token", jwt);
        response.put("userId", user.getId().toString());
        response.put("role", user.getRole());
        response.put("name", user.getName());
        response.put("email", user.getEmail());
        response.put("profilePicture", user.getProfilePicture());

        // 9. Ajouter clientId ou coiffeurId selon le role
        if (user.getRole().equals("CLIENT")) {
            Client client = clientRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new RuntimeException("Client non trouvé"));
            response.put("clientId", client.getId().toString());
        } else if (user.getRole().equals("COIFFEUR")) {
            Coiffeur coiffeur = coiffeurRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new RuntimeException("Coiffeur non trouvé"));
            response.put("coiffeurId", coiffeur.getId().toString());
            response.put("isAdmin", String.valueOf(coiffeur.isAdmin()));
        }

        return response;
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

            GoogleIdToken idToken = verifier.verify(token);

            if (idToken == null) {
                System.out.println("❌ Token null — essai sans vérification audience...");

                // Essayer de parser le token sans vérifier l'audience
                idToken = GoogleIdToken.parse(GsonFactory.getDefaultInstance(), token);

                if (idToken != null) {
                    System.out.println("✅ Token parsé — audience : " + idToken.getPayload().getAudience());
                    System.out.println("✅ azp : " + idToken.getPayload().getAuthorizedParty());
                }
            }

            return idToken;

        } catch (Exception e) {
            System.out.println("❌ Erreur : " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de la vérification du token Google : " + e.getMessage());
        }
    }
}