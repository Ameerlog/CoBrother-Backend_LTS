package com.cobrother.web.service.venture;

import com.cobrother.web.Entity.coventure.Venture;
import com.cobrother.web.Entity.coventure.VentureRole;
import com.cobrother.web.Entity.user.AppUser;
import com.cobrother.web.Repository.VentureRepository;
import com.cobrother.web.model.venture.VentureDto;
import com.cobrother.web.service.auth.CurrentUserService;
import com.cobrother.web.service.notification.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class VentureService {

    @Autowired
    private VentureRepository ventureRepository;

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private NotificationService notificationService;

    // ─────────────────────────────────────────────────────────────────────────
    // Queries
    // ─────────────────────────────────────────────────────────────────────────

    public List<Venture> getAllVentures() {
        return ventureRepository.findAll();
    }

    public List<Venture> getVenturesByUser(AppUser user) {
        return ventureRepository.findByListedBy(user);
    }

    public Venture getVentureEntity(long id) {
        return ventureRepository.findById(id)
                .orElseThrow(() -> new VentureNotFoundException("Venture not found: " + id));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Create
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public Venture addVentureEntity(Venture venture, List<VentureDto.VentureRoleDto> roleDtos) {
        venture.getRoles().clear();
        if (roleDtos != null) {
            for (int i = 0; i < roleDtos.size(); i++) {
                VentureRole role = roleDtos.get(i).toEntity();
                role.setVenture(venture);
                role.setSortOrder(i);
                venture.getRoles().add(role);
            }
        }

        Venture saved = ventureRepository.save(venture);

        if (saved.getBrandDetails() != null && saved.getBrandDetails().getIndustry() != null) {
            notificationService.notifyIndustryUsersOfNewListing(
                    saved.getBrandDetails().getIndustry().name(),
                    saved.getBrandDetails().getBrandName(),
                    "Venture",
                    "/ventures",
                    saved.getListedBy()
            );
        }
        return saved;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Update
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public Venture updateVentureEntity(long id, Venture incoming,
                                       List<VentureDto.VentureRoleDto> roleDtos) {
        Venture existing = getVentureEntity(id);
        AppUser currentUser = currentUserService.getCurrentUser();

        if (!existing.getListedBy().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You are not authorized to edit this venture.");
        }

        // Preserve S3 image URL — never overwrite from PUT body
        String existingImageUrl = existing.getBrandDetails() != null
                ? existing.getBrandDetails().getVentureImageUrl() : null;

        existing.setBrandDetails(incoming.getBrandDetails());
        existing.setContactInfo(incoming.getContactInfo());
        existing.setAgreement(incoming.getAgreement());
        existing.setStatus(incoming.isStatus());
        existing.setStage(incoming.getStage());
        existing.setCurrentProblem(incoming.getCurrentProblem());

        if (existing.getBrandDetails() != null && existingImageUrl != null) {
            existing.getBrandDetails().setVentureImageUrl(existingImageUrl);
        }

        // Full role replacement — orphanRemoval handles deletes automatically
        existing.getRoles().clear();
        if (roleDtos != null) {
            for (int i = 0; i < roleDtos.size(); i++) {
                VentureRole role = roleDtos.get(i).toEntity();
                role.setVenture(existing);
                role.setSortOrder(i);
                existing.getRoles().add(role);
            }
        }

        return ventureRepository.save(existing);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Delete
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public void deleteVenture(long id) {
        Venture existing = getVentureEntity(id);
        AppUser currentUser = currentUserService.getCurrentUser();

        if (!existing.getListedBy().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You are not authorized to delete this venture.");
        }

        existing.getRoles().clear();
        existing.getCoVentureApplications().clear();
        ventureRepository.delete(existing);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Image helper — no role changes involved
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public Venture saveVentureEntity(Venture venture) {
        return ventureRepository.save(venture);
    }
}