package com.cobrother.web.service.venture;

import com.cobrother.web.Entity.coventure.CoVenture;
import com.cobrother.web.Entity.coventure.CoVentureStatus;
import com.cobrother.web.Entity.coventure.Venture;
import com.cobrother.web.Entity.user.AppUser;
import com.cobrother.web.Repository.CoVentureRepository;
import com.cobrother.web.Repository.VentureRepository;
import com.cobrother.web.service.auth.CurrentUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class CoVentureService {

    @Autowired
    private CoVentureRepository coVentureRepository;

    @Autowired
    private VentureRepository ventureRepository;

    @Autowired
    private CurrentUserService currentUserService;

    /**
     * Apply to co-venture into a listed venture.
     *
     * Rules:
     *  - The venture must exist and be active (status = true).
     *  - The applicant cannot be the one who listed the venture.
     *  - The applicant cannot apply more than once to the same venture.
     *
     * On success:
     *  - A CoVenture record is saved with status PENDING.
     *  - The venture's coVentureApplicationCount is incremented.
     *  - The CoVenture is linked to the applicant (surfacing in their purchasedVentures
     *    via the coVenturedVentures relation on AppUser).
     */
    public ResponseEntity<?> applyToCoVenture(long ventureId, CoVenture application) {
        try {
            Venture venture = ventureRepository.getVentureById(ventureId);

            if (venture == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Venture not found.");
            }

            if (!venture.isStatus()) {
                return ResponseEntity.status(HttpStatus.GONE)
                        .body("This venture is no longer active.");
            }

            AppUser currentUser = currentUserService.getCurrentUser();

            // Guard: lister cannot apply to their own venture
            if (venture.getListedBy().getId().equals(currentUser.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("You cannot co-venture into a venture you have listed.");
            }

            // Guard: prevent duplicate applications
            if (coVentureRepository.existsByVentureAndApplicant(venture, currentUser)) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body("You have already applied to co-venture into this venture.");
            }

            // Build and save the application
            application.setVenture(venture);
            application.setApplicant(currentUser);
            application.setStatus(CoVentureStatus.PENDING);

            CoVenture saved = coVentureRepository.save(application);

            // Increment the application counter on the venture
            venture.setCoVentureApplicationCount(venture.getCoVentureApplicationCount() + 1);
            ventureRepository.save(venture);

            return ResponseEntity.ok(saved);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Something went wrong while processing your application.");
        }
    }
}