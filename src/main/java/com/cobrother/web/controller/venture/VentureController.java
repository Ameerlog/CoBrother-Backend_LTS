package com.cobrother.web.controller.venture;

import com.cobrother.web.Entity.coventure.Venture;
import com.cobrother.web.Entity.user.AppUser;
import com.cobrother.web.model.venture.VentureDto;
import com.cobrother.web.service.S3Service;
import com.cobrother.web.service.analytics.AnalyticsService;
import com.cobrother.web.service.auth.CurrentUserService;
import com.cobrother.web.service.venture.VentureService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("api/v1/venture")
public class VentureController {

    @Autowired
    public VentureService ventureService;

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private S3Service s3Service;

    // ── Exception handler: ownership violations → 403 ─────────────────────────
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(403).body(Map.of("error", ex.getMessage()));
    }

    // ── Exception handler: venture not found → 404 ─────────────────────────────
    @ExceptionHandler(VentureNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleVentureNotFound(VentureNotFoundException ex) {
        return ResponseEntity.status(404).body(Map.of("error", ex.getMessage()));
    }

    // ── GET /all ──────────────────────────────────────────────────────────────
    @GetMapping("/all")
    public ResponseEntity<List<VentureDto>> getAllVentures() {
        AppUser currentUser = null;
        try {
            currentUser = currentUserService.getCurrentUser();
        } catch (Exception ignored) {}
        
        final AppUser user = currentUser;
        return ResponseEntity.ok(
                ventureService.getAllVentures().stream()
                        .map(v -> VentureDto.from(v, user))
                        .collect(Collectors.toList())
        );
    }

    // ── GET /my ───────────────────────────────────────────────────────────────
    @GetMapping("/my")
    public ResponseEntity<List<VentureDto>> getMyVentures() {
        AppUser currentUser = currentUserService.getCurrentUser();
        return ResponseEntity.ok(
                ventureService.getVenturesByUser(currentUser).stream()
                        .map(v -> VentureDto.from(v, currentUser))
                        .collect(Collectors.toList())
        );
    }

    // ── GET /{id} ─────────────────────────────────────────────────────────────
    @GetMapping("/{id}")
    public ResponseEntity<VentureDto> getVenture(@PathVariable long id) {
        Venture v = ventureService.getVentureEntity(id);
        AppUser currentUser = null;
        try {
            currentUser = currentUserService.getCurrentUser();
            analyticsService.trackVentureView(v, currentUser);
        } catch (Exception ignored) {}
        return ResponseEntity.ok(VentureDto.from(v, currentUser));
    }

    // ── POST / ────────────────────────────────────────────────────────────────
    @PostMapping
    public ResponseEntity<VentureDto> addVenture(@RequestBody VentureDto dto) {
        AppUser currentUser = currentUserService.getCurrentUser();
        Venture venture = dto.toEntity();
        venture.setListedBy(currentUser);
        Venture saved = ventureService.addVentureEntity(venture, dto.getRoles());
        return ResponseEntity.ok(VentureDto.from(saved, currentUser));
    }

    // ── PUT /{id} ─────────────────────────────────────────────────────────────
    @PutMapping("/{id}")
    public ResponseEntity<VentureDto> updateVenture(
            @PathVariable long id, @RequestBody VentureDto dto) {
        AppUser currentUser = currentUserService.getCurrentUser();
        Venture incoming = dto.toEntity();
        Venture updated = ventureService.updateVentureEntity(id, incoming, dto.getRoles());
        return ResponseEntity.ok(VentureDto.from(updated, currentUser));
    }

    // ── DELETE /{id} ──────────────────────────────────────────────────────────
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVenture(@PathVariable long id) {
        ventureService.deleteVenture(id);
        return ResponseEntity.noContent().build();
    }

    // ── POST /{id}/image ──────────────────────────────────────────────────────
    @PostMapping("/{id}/image")
    public ResponseEntity<?> uploadVentureImage(
            @PathVariable long id,
            @RequestParam("file") MultipartFile file) {

        Venture venture = ventureService.getVentureEntity(id);
        AppUser current = currentUserService.getCurrentUser();

        if (!venture.getListedBy().getId().equals(current.getId())) {
            return ResponseEntity.status(403)
                    .body(Map.of("error", "Not authorized to upload image for this venture"));
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Only image files are allowed"));
        }

        if (venture.getBrandDetails() != null
                && venture.getBrandDetails().getVentureImageUrl() != null) {
            s3Service.deleteVentureImage(venture.getBrandDetails().getVentureImageUrl());
        }

        try {
            String imageUrl = s3Service.uploadVentureImage(file, id);
            venture.getBrandDetails().setVentureImageUrl(imageUrl);
            ventureService.saveVentureEntity(venture);
            return ResponseEntity.ok(Map.of("ventureImageUrl", imageUrl));
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Upload failed: " + e.getMessage()));
        }
    }
}