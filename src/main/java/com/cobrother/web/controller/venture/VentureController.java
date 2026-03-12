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
    /**
     * GET /api/v1/venture/all
     * Returns all ventures as safe DTOs (no circular references)
     */
    @GetMapping("/all")
    public ResponseEntity<List<VentureDto>> getAllVentures() {
        List<Venture> ventures = ventureService.getAllVentures();
        List<VentureDto> dtos = ventures.stream()
                .map(VentureDto::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /**
     * GET /api/v1/venture/my
     * Returns ventures listed by the current user
     */
    @GetMapping("/my")
    public ResponseEntity<List<VentureDto>> getMyVentures() {
        List<Venture> ventures = ventureService.getVenturesByUser(currentUserService.getCurrentUser());
        List<VentureDto> dtos = ventures.stream()
                .map(VentureDto::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<VentureDto> getVenture(@PathVariable long id) {
        Venture v = ventureService.getVentureEntity(id);
        // ✅ Track view
        try {
            AppUser viewer = currentUserService.getCurrentUser();
            analyticsService.trackVentureView(v, viewer);
        } catch (Exception ignored) {}
        return ResponseEntity.ok(VentureDto.from(v));
    }

    /**
     * POST /api/v1/venture
     */
    @PostMapping
    public ResponseEntity<VentureDto> addVenture(@RequestBody Venture venture) {
        venture.setListedBy(currentUserService.getCurrentUser());
        Venture saved = ventureService.addVentureEntity(venture);
        return ResponseEntity.ok(VentureDto.from(saved));
    }

    /**
     * PUT /api/v1/venture/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<VentureDto> updateVenture(@PathVariable long id, @RequestBody Venture venture) {
        Venture updated = ventureService.updateVentureEntity(id, venture);
        return ResponseEntity.ok(VentureDto.from(updated));
    }

    /**
     * DELETE /api/v1/venture/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVenture(@PathVariable long id) {
        ventureService.deleteVenture(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/image")
    public ResponseEntity<?> uploadVentureImage(
            @PathVariable long id,
            @RequestParam("file") MultipartFile file) {

        Venture venture = ventureService.getVentureEntity(id);

        // Only owner can upload
        AppUser current = currentUserService.getCurrentUser();
        if (!venture.getListedBy().getId().equals(current.getId())) {
            return ResponseEntity.status(403).body("Not authorized");
        }

        // Validate file type
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return ResponseEntity.badRequest().body("Only image files are allowed");
        }

        // Delete old image if exists
        if (venture.getBrandDetails() != null
                && venture.getBrandDetails().getVentureImageUrl() != null) {
            s3Service.deleteVentureImage(venture.getBrandDetails().getVentureImageUrl());
        }

        try {
            String imageUrl = s3Service.uploadVentureImage(file, id);
            venture.getBrandDetails().setVentureImageUrl(imageUrl);
            ventureService.addVentureEntity(venture);
            return ResponseEntity.ok(Map.of("ventureImageUrl", imageUrl));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Upload failed: " + e.getMessage());
        }
    }
}