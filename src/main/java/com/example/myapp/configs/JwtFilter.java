package com.example.myapp.configs;

import com.example.myapp.services.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // 1. Récupérer le header "Authorization"
        String authHeader = request.getHeader("Authorization");

        // 2. Si pas de header ou ne commence pas par "Bearer " → passer au filtre suivant
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // 3. Extraire le token (enlever "Bearer " au début)
            String token = authHeader.substring(7);

            // 4. Vérifier que le token est valide
            if (!jwtService.isTokenValid(token)) {
                filterChain.doFilter(request, response);
                return;
            }
            // Après validation du token
            request.setAttribute("userId", jwtService.extractUserId(token));
            request.setAttribute("role", jwtService.extractRole(token));

            // 5. Extraire les infos depuis le token
            String email = jwtService.extractEmail(token);
            String role = jwtService.extractRole(token);

            // 6. Créer l'authentification Spring Security
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            email,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + role))
                    );

            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            // 7. Enregistrer l'authentification dans le SecurityContext
            // → Spring Security sait maintenant qui est connecté
            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (Exception e) {
            // Token invalide → on ne fait rien, la requête sera bloquée si l'endpoint est protégé
        }

        // 8. Passer au filtre suivant
        filterChain.doFilter(request, response);
    }
}