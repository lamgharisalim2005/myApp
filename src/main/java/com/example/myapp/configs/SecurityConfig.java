package com.example.myapp.configs;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Désactiver CSRF car on utilise JWT (pas de sessions)
                .csrf(csrf -> csrf.disable())

                // Pas de sessions — JWT gère l'authentification
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .authorizeHttpRequests(auth -> auth

                        // ========== ENDPOINTS PUBLICS (sans token) ==========
                        .requestMatchers(
                                "/api/auth/**",              // connexion/inscription
                                "/swagger-ui/**",            // documentation Swagger
                                "/v3/api-docs/**",           // API docs JSON
                                "/ws/**"                     // WebSocket
                        ).permitAll()

                        // ========== ENDPOINTS CLIENT ET COIFFEUR ==========
                        .requestMatchers(HttpMethod.GET, "/api/salons").hasAnyRole("CLIENT", "COIFFEUR")
                        .requestMatchers(HttpMethod.GET, "/api/salons/*/detail").hasAnyRole("CLIENT", "COIFFEUR")
                        .requestMatchers(HttpMethod.GET, "/api/salons/*/photos").hasAnyRole("CLIENT", "COIFFEUR")
                        .requestMatchers(HttpMethod.GET, "/api/coiffeurs/*/detail").hasAnyRole("CLIENT", "COIFFEUR")
                        .requestMatchers(HttpMethod.GET, "/api/coiffeurs/*/photos").hasAnyRole("CLIENT", "COIFFEUR")
                        .requestMatchers(HttpMethod.GET, "/api/workschedules/coiffeur/*").hasAnyRole("CLIENT", "COIFFEUR")
                        .requestMatchers("/api/messages/**").hasAnyRole("CLIENT", "COIFFEUR")
                        .requestMatchers("/api/notifications/**").hasAnyRole("CLIENT", "COIFFEUR")
                        .requestMatchers(HttpMethod.GET, "/api/messages/online/*").hasAnyRole("CLIENT", "COIFFEUR")
                        .requestMatchers(HttpMethod.DELETE, "/api/reservations/*").hasAnyRole("CLIENT", "COIFFEUR")
                        .requestMatchers(HttpMethod.GET, "/api/clients/*/public").hasAnyRole("CLIENT", "COIFFEUR")
                        .requestMatchers(HttpMethod.GET, "/api/coiffeurs/*/public").hasAnyRole("CLIENT", "COIFFEUR")
                        .requestMatchers("/api/blocks/**").hasAnyRole("CLIENT", "COIFFEUR")

                        // Webhook Stripe
                        .requestMatchers("/api/payments/webhook").permitAll()

                        // ========== ENDPOINTS CLIENT UNIQUEMENT ==========
                        .requestMatchers("/api/clients/**").hasAnyRole("CLIENT")
                        .requestMatchers(HttpMethod.POST, "/api/reservations").hasAnyRole("CLIENT")
                        .requestMatchers("/api/payments/intent").hasAnyRole("CLIENT")
                        .requestMatchers(HttpMethod.GET, "/api/reservations/coiffeur/*/slots").hasRole("CLIENT")
                        // CLIENT — Annuler et voir ses réservations
                        .requestMatchers(HttpMethod.GET, "/api/reservations/client").hasAnyRole("CLIENT")
                        .requestMatchers(HttpMethod.PUT, "/api/reservations/*/cancel").hasAnyRole("CLIENT")


                        // ========== ENDPOINTS COIFFEUR UNIQUEMENT ==========
                        .requestMatchers("/api/services/**").hasAnyRole("COIFFEUR")
                        .requestMatchers("/api/workschedules/**").hasAnyRole("COIFFEUR")
                        .requestMatchers(HttpMethod.POST, "/api/salons").hasAnyRole("COIFFEUR")
                        .requestMatchers(HttpMethod.DELETE, "/api/salons/**").hasAnyRole("COIFFEUR")
                        .requestMatchers(HttpMethod.PUT, "/api/salons/**").hasAnyRole("COIFFEUR")
                        .requestMatchers(HttpMethod.POST, "/api/salons/*/photos").hasAnyRole("COIFFEUR")
                        .requestMatchers(HttpMethod.DELETE, "/api/salons/**").hasAnyRole("COIFFEUR")
                        .requestMatchers("/api/salon-requests/**").hasAnyRole("COIFFEUR")
                        .requestMatchers("/api/coiffeurs/*/profile").hasAnyRole("COIFFEUR")
                        .requestMatchers(HttpMethod.POST, "/api/coiffeurs/*/photos").hasAnyRole("COIFFEUR")
                        .requestMatchers(HttpMethod.DELETE, "/api/coiffeurs/**").hasAnyRole("COIFFEUR")
                        .requestMatchers(HttpMethod.PUT, "/api/coiffeurs/*/quitter-salon").hasAnyRole("COIFFEUR")
                        .requestMatchers(HttpMethod.GET, "/api/reservations/coiffeur").hasAnyRole("COIFFEUR")


                        // ========== TOUT LE RESTE NÉCESSITE AUTH ==========
                        .anyRequest().authenticated()
                )

                // Ajouter notre JwtFilter avant le filtre d'authentification par défaut
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}