package com.cobrother.web.controller.venture;

import com.cobrother.web.Entity.coventure.CoVenture;
import com.cobrother.web.Entity.coventure.CoVentureStatus;
import com.cobrother.web.Entity.user.AppUser;
import com.cobrother.web.Repository.CoVentureRepository;
import com.cobrother.web.Repository.VentureRepository;
import com.cobrother.web.service.auth.CurrentUserService;
import com.cobrother.web.service.venture.CoVentureService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("api/v1/coventure")
public class CoVentureController {

    @Autowired
    private CoVentureService coVentureService;

    @Autowired
    private CoVentureRepository coVentureRepository;

    @Autowired
    private VentureRepository ventureRepository;

    @Autowired
    private CurrentUserService currentUserService;

    /**
     * GET /api/v1/coventure/{ventureId}/my-status
     *
     * Returns whether the current user has already applied to this venture.
     * Response: { "applied": true, "status": "PENDING" }
     *        or { "applied": false }
     */
    @GetMapping("/{ventureId}/my-status")
    public ResponseEntity<?> checkMyStatus(@PathVariable long ventureId) {
        AppUser currentUser = currentUserService.getCurrentUser();

        Optional<CoVenture> existing = coVentureRepository
                .findByVentureIdAndApplicantId(ventureId, currentUser.getId());

        if (existing.isPresent()) {
            return ResponseEntity.ok(Map.of(
                    "applied", true,
                    "status", existing.get().getStatus().name()
            ));
        }
        return ResponseEntity.ok(Map.of("applied", false));
    }

    /**
     * POST /api/v1/coventure/{ventureId}
     *
     * Apply to co-venture. Returns 409 CONFLICT if the user has already applied.
     */
    @PostMapping("/{ventureId}")
    public ResponseEntity<?> applyToCoVenture(
            @PathVariable long ventureId,
            @RequestBody CoVenture application) {

        AppUser currentUser = currentUserService.getCurrentUser();

        // ── Duplicate guard ───────────────────────────────────────────────
        boolean alreadyApplied = coVentureRepository
                .existsByVentureIdAndApplicantId(ventureId, currentUser.getId());

        if (alreadyApplied) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(Map.of(
                            "error", "You have already applied to this venture.",
                            "code",  "DUPLICATE_APPLICATION"
                    ));
        }

        // ── Validate venture exists ───────────────────────────────────────
        if (!ventureRepository.existsById(ventureId)) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Venture not found."));
        }

        return coVentureService.applyToCoVenture(ventureId, application);
    }

    /**
     * GET /api/v1/coventure/my-applications
     * Returns all co-venture applications made by the current user
     */
    @GetMapping("/my-applications")
    public ResponseEntity<?> getMyApplications() {
        AppUser currentUser = currentUserService.getCurrentUser();
        List<CoVenture> applications = coVentureRepository.findByApplicantId(currentUser.getId());
        return ResponseEntity.ok(applications);
    }

    /**
     * GET /api/v1/coventure/my-venture-applications
     * Returns all applications made to ventures listed by the current user
     */
    @GetMapping("/my-venture-applications")
    public ResponseEntity<?> getMyVentureApplications(
            @RequestParam(required = false) String status) {
        AppUser currentUser = currentUserService.getCurrentUser();
        List<CoVenture> applications;
        if (status != null && !status.isBlank()) {
            CoVentureStatus s = CoVentureStatus.valueOf(status.toUpperCase());
            applications = coVentureRepository
                    .findByVentureListedByIdAndStatus(currentUser.getId(), s);
        } else {
            applications = coVentureRepository
                    .findByVentureListedById(currentUser.getId());
        }
        return ResponseEntity.ok(applications);
    }

    /**
     * PUT /api/v1/coventure/{id}/status
     * Approve or reject an application (only venture owner can do this)
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateApplicationStatus(
            @PathVariable long id,
            @RequestBody Map<String, String> body) {
        AppUser currentUser = currentUserService.getCurrentUser();
        Optional<CoVenture> opt = coVentureRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Application not found."));
        }
        CoVenture cv = opt.get();
        // Only the venture owner can approve/reject
        if (!cv.getVenture().getListedBy().getId().equals(currentUser.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Only the venture owner can update application status."));
        }
        try {
            CoVentureStatus newStatus = CoVentureStatus.valueOf(body.get("status").toUpperCase());
            cv.setStatus(newStatus);
            coVentureRepository.save(cv);
            return ResponseEntity.ok(Map.of("success", true, "status", newStatus.name()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid status value."));
        }
    }
}