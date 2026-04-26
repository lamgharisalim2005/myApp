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

                        // GET publics — voir les infos publiques sans compte
                        .requestMatchers(HttpMethod.GET, "/api/salons").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/salons/*/detail").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/salons/*/photos").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/coiffeurs/*/detail").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/coiffeurs/*/photos").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/workschedules/coiffeur/*").permitAll()

                        // Webhook Stripe
                        .requestMatchers("/api/payments/webhook").permitAll()

                        // ========== ENDPOINTS CLIENT UNIQUEMENT ==========
                        .requestMatchers("/api/clients/**").hasAnyRole("CLIENT", "ROOT")
                        .requestMatchers(HttpMethod.POST, "/api/reservations").hasAnyRole("CLIENT", "ROOT")
                        .requestMatchers("/api/payments/intent").hasAnyRole("CLIENT", "ROOT")

                        // ========== ENDPOINTS COIFFEUR UNIQUEMENT ==========
                        .requestMatchers("/api/services/**").hasAnyRole("COIFFEUR", "ROOT")
                        .requestMatchers("/api/workschedules/**").hasAnyRole("COIFFEUR", "ROOT")
                        .requestMatchers(HttpMethod.POST, "/api/salons").hasAnyRole("COIFFEUR", "ROOT")
                        .requestMatchers(HttpMethod.DELETE, "/api/salons/**").hasAnyRole("COIFFEUR", "ROOT")
                        .requestMatchers(HttpMethod.PUT, "/api/salons/**").hasAnyRole("COIFFEUR", "ROOT")
                        .requestMatchers(HttpMethod.POST, "/api/salons/*/photos").hasAnyRole("COIFFEUR", "ROOT")
                        .requestMatchers(HttpMethod.DELETE, "/api/salons/**").hasAnyRole("COIFFEUR", "ROOT")
                        .requestMatchers("/api/salon-requests/**").hasAnyRole("COIFFEUR", "ROOT")
                        .requestMatchers("/api/coiffeurs/*/profile").hasAnyRole("COIFFEUR", "ROOT")
                        .requestMatchers(HttpMethod.POST, "/api/coiffeurs/*/photos").hasAnyRole("COIFFEUR", "ROOT")
                        .requestMatchers(HttpMethod.DELETE, "/api/coiffeurs/**").hasAnyRole("COIFFEUR", "ROOT")
                        .requestMatchers(HttpMethod.PUT, "/api/coiffeurs/*/quitter-salon").hasAnyRole("COIFFEUR", "ROOT")
                        .requestMatchers(HttpMethod.PUT, "/api/reservations/*").hasAnyRole("COIFFEUR", "ROOT")

                        // ========== ENDPOINTS CLIENT ET COIFFEUR ==========
                        .requestMatchers("/api/messages/**").hasAnyRole("CLIENT", "COIFFEUR", "ROOT")
                        .requestMatchers("/api/notifications/**").hasAnyRole("CLIENT", "COIFFEUR", "ROOT")


                        // ========== TOUT LE RESTE NÉCESSITE AUTH ==========
                        .anyRequest().authenticated()
                )

                // Ajouter notre JwtFilter avant le filtre d'authentification par défaut
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}