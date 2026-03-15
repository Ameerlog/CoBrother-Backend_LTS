package com.cobrother.web.service.notification;

import com.cobrother.web.Entity.notification.Notification;
import com.cobrother.web.Entity.notification.NotificationType;
import com.cobrother.web.Entity.user.AppUser;
import com.cobrother.web.Repository.CommunityRepository;
import com.cobrother.web.Repository.NotificationRepository;
import com.cobrother.web.Repository.UserRepository;
import com.cobrother.web.service.auth.CurrentUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class NotificationService {

    @Autowired private NotificationRepository notificationRepository;
    @Autowired private CurrentUserService currentUserService;

    @Autowired private CommunityRepository communityRepository;
    @Autowired private UserRepository userRepository;


    // ── Create helpers (called from other services) ───────────────────────────

    public void notify(AppUser recipient, NotificationType type,
                       String title, String message, String link) {
        notificationRepository.save(
                new Notification(recipient, type, title, message, link)
        );
    }

    // ── New CoVenture application received (notify venture owner) ─────────────
    public void notifyCoVentureApplicationReceived(AppUser ventureOwner,
                                                   String applicantName,
                                                   String ventureName,
                                                   Long ventureId) {
        notify(ventureOwner,
                NotificationType.COVENTURE_APPLICATION_RECEIVED,
                "New Application",
                applicantName + " applied to co-venture on " + ventureName,
                "/ventures/dashboard"
        );
    }

    // ── CoVenture status changed (notify applicant) ───────────────────────────
    public void notifyCoVentureStatusChanged(AppUser applicant,
                                             String ventureName,
                                             String newStatus) {
        String statusLabel = newStatus.equalsIgnoreCase("ACCEPTED") ? "accepted ✓" : "rejected";
        notify(applicant,
                NotificationType.COVENTURE_APPLICATION_STATUS_CHANGED,
                "Application " + (newStatus.equalsIgnoreCase("ACCEPTED") ? "Accepted" : "Rejected"),
                "Your application to ventureName was " + statusLabel,
                "/ventures/dashboard"
        );
    }

    // ── Domain sold (notify seller) ───────────────────────────────────────────
    public void notifyDomainSold(AppUser seller, String domainName,
                                 String buyerName) {
        notify(seller,
                NotificationType.DOMAIN_SOLD,
                "Domain Sold",
                buyerName + " purchased your domain "+ domainName,
                "/domains/dashboard"
        );
    }

    // ── Software purchased (notify seller) ───────────────────────────────────
    public void notifySoftwarePurchased(AppUser seller, String softwareName,
                                        String buyerName) {
        notify(seller,
                NotificationType.SOFTWARE_PURCHASED,
                "Software Sold",
                buyerName + " purchased your software "+ softwareName,
                "/cocreation/dashboard"
        );
    }

    // ── Software marked complete (notify seller) ──────────────────────────────
    public void notifySoftwareMarkedComplete(AppUser seller, String softwareName,
                                             String buyerName) {
        notify(seller,
                NotificationType.SOFTWARE_MARKED_COMPLETE,
                "Purchase Confirmed",
                buyerName + " marked " + softwareName + " as complete",
                "/cocreation/dashboard"
        );
    }

    // ── Profile viewed (notify profile owner) ─────────────────────────────────
    public void notifyProfileViewed(AppUser profileOwner, String viewerName) {
        notify(profileOwner,
                NotificationType.PROFILE_VIEWED,
                "Profile Viewed",
                viewerName + " viewed your profile",
                "/profile/analytics"
        );
    }

    // ── New listing in user's industry (notify community member) ─────────────
    public void notifyNewListingInIndustry(AppUser user, String listingName,
                                           String listingType, String link) {
        notify(user,
                NotificationType.NEW_LISTING_IN_INDUSTRY,
                "New " + listingType + " in Your Industry",
                listingName + " was just listed — check it out",
                link
            );
        }
    
        // ── API methods ───────────────────────────────────────────────────────────
    
    public ResponseEntity<List<Notification>> getRecent() {
        AppUser me = currentUserService.getCurrentUser();
        return ResponseEntity.ok(
            notificationRepository.findByRecipientOrderByCreatedAtDesc(
                me, PageRequest.of(0, 10)
            )
        );
    }

    public ResponseEntity<List<Notification>> getAll() {
        AppUser me = currentUserService.getCurrentUser();
        return ResponseEntity.ok(
            notificationRepository.findByRecipientOrderByCreatedAtDesc(me)
        );
    }

    public ResponseEntity<Map<String, Long>> getUnreadCount() {
        AppUser me = currentUserService.getCurrentUser();
        return ResponseEntity.ok(
            Map.of("count", notificationRepository.countByRecipientAndIsReadFalse(me))
        );
    }

    public ResponseEntity<?> markAllRead() {
        AppUser me = currentUserService.getCurrentUser();
        notificationRepository.markAllReadByRecipient(me);
        return ResponseEntity.ok(Map.of("success", true));
    }

    public ResponseEntity<?> markOneRead(Long id) {
        AppUser me = currentUserService.getCurrentUser();
        return notificationRepository.findById(id).map(n -> {
            if (!n.getRecipient().getId().equals(me.getId()))
                return ResponseEntity.status(403).build();
            n.setRead(true);
            notificationRepository.save(n);
            return ResponseEntity.ok().build();
        }).orElse(ResponseEntity.notFound().build());
    }

    public void notifyIndustryUsersOfNewListing(String industryName, String listingName,
                                                String listingType, String link,
                                                AppUser lister) {
        // Find all community profiles in the same industry
        communityRepository.findAll().stream()
                .filter(c -> c.getIndustry() != null
                        && c.getIndustry().name().equalsIgnoreCase(industryName)
                        && !c.getAppUser().getId().equals(lister.getId()))
                .forEach(c -> notifyNewListingInIndustry(
                        c.getAppUser(), listingName, listingType, link
                ));
    }
}


