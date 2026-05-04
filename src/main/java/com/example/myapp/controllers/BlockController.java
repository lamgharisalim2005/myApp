package com.example.myapp.controllers;

import com.example.myapp.entitys.Block;
import com.example.myapp.exceptions.GlobalResponse;
import com.example.myapp.repositories.BlockRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/blocks")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class BlockController {

    private final BlockRepository blockRepository;

    // Bloquer un utilisateur
    @PostMapping("/{blockedId}")
    public ResponseEntity<GlobalResponse<String>> bloquer(
            @PathVariable UUID blockedId,
            HttpServletRequest httpRequest) {
        UUID blockerId = (UUID) httpRequest.getAttribute("userId");

        if (blockerId.equals(blockedId)) {
            throw new RuntimeException("Vous ne pouvez pas vous bloquer vous-même");
        }

        if (!blockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId)) {
            Block block = new Block();
            block.setBlockerId(blockerId);
            block.setBlockedId(blockedId);
            block.setCreatedAt(LocalDateTime.now());
            blockRepository.save(block);
        }

        return ResponseEntity.ok(new GlobalResponse<>("Utilisateur bloqué"));
    }

    // Débloquer un utilisateur
    @DeleteMapping("/{blockedId}")
    public ResponseEntity<GlobalResponse<String>> debloquer(
            @PathVariable UUID blockedId,
            HttpServletRequest httpRequest) {
        UUID blockerId = (UUID) httpRequest.getAttribute("userId");

        blockRepository.findByBlockerIdAndBlockedId(blockerId, blockedId)
                .ifPresent(blockRepository::delete);

        return ResponseEntity.ok(new GlobalResponse<>("Utilisateur débloqué"));
    }

    // Vérifier si un utilisateur est bloqué
    @GetMapping("/{userId}")
    public ResponseEntity<GlobalResponse<Boolean>> estBloque(
            @PathVariable UUID userId,
            HttpServletRequest httpRequest) {
        UUID currentUserId = (UUID) httpRequest.getAttribute("userId");
        boolean bloque = blockRepository.existsByBlockerIdAndBlockedId(currentUserId, userId);
        return ResponseEntity.ok(new GlobalResponse<>(bloque));
    }
}