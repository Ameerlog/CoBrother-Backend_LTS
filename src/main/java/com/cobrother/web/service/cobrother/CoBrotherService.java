package com.cobrother.web.service.cobrother;

import com.cobrother.web.Entity.cobrother.CoBrotherRequest;
import com.cobrother.web.Entity.cobrother.CoBrotherRequestStatus;
import com.cobrother.web.Entity.user.AppUser;
import com.cobrother.web.Repository.CoBrotherRequestRepository;
import com.cobrother.web.service.auth.MailService;
import com.cobrother.web.service.notification.NotificationService;
import com.cobrother.web.Entity.notification.NotificationType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class CoBrotherService {

    @Autowired private CoBrotherRequestRepository requestRepository;
    @Autowired private MailService mailService;
    @Autowired private NotificationService notificationService;

    // ── All requests assigned to this CoBrother ───────────────────────────────
    public ResponseEntity<?> getMyRequests(AppUser coBrother) {
        List<CoBrotherRequest> requests =
                requestRepository.findByAssignedCoBrotherOrderByCreatedAtDesc(coBrother);
        return ResponseEntity.ok(requests);
    }

    // ── CoBrother accepts or rejects a request ────────────────────────────────
    public ResponseEntity<?> respond(Long requestId, boolean accepted,
                                     String note, AppUser coBrother) {

        CoBrotherRequest request = requestRepository.findById(requestId).orElse(null);
        if (request == null)
            return ResponseEntity.notFound().build();

        if (!request.getAssignedCoBrother().getId().equals(coBrother.getId()))
            return ResponseEntity.status(403).body("Not assigned to you");

        if (request.getStatus() != CoBrotherRequestStatus.FORWARDED)
            return ResponseEntity.badRequest().body("Request is not in a respondable state");

        request.setStatus(accepted
                ? CoBrotherRequestStatus.ACCEPTED
                : CoBrotherRequestStatus.REJECTED);
        request.setCoBrotherNote(note);
        request.setRespondedAt(LocalDateTime.now());
        requestRepository.save(request);

        // ── If accepted: auto-cancel every other active request for the same entity
        //    so the lister cannot end up with multiple accepted CoBrothers.
        if (accepted) {
            List<CoBrotherRequest> otherRequests = requestRepository
                    .findByEntityIdAndRequestType(request.getEntityId(), request.getRequestType())
                    .stream()
                    .filter(r -> !r.getId().equals(requestId))
                    .filter(r -> r.getStatus() == CoBrotherRequestStatus.FORWARDED
                            || r.getStatus() == CoBrotherRequestStatus.PAYMENT_PENDING)
                    .collect(java.util.stream.Collectors.toList());

            otherRequests.forEach(r -> {
                r.setStatus(CoBrotherRequestStatus.CANCELLED);
                requestRepository.save(r);

                // Let the other CoBrother know their request was closed
                notificationService.notify(
                        r.getAssignedCoBrother(),
                        NotificationType.COVENTURE_APPLICATION_STATUS_CHANGED,
                        "Request Closed",
                        "The request for " + r.getEntityTitle() +
                                " has been handled by another CoBrother.",
                        "/cobrother"
                );
            });
        }

        // ── Notify lister by email ────────────────────────────────────────────
        String outcome = accepted ? "accepted" : "rejected";
        mailService.sendCoBrotherResponseEmail(
                request.getListerEmail(),
                request.getListerName(),
                request.getEntityTitle(),
                accepted,
                note
        );

        // ── Notify lister via in-app notification ─────────────────────────────
        if (request.getLister() != null) {
            notificationService.notify(
                    request.getLister(),
                    NotificationType.COVENTURE_APPLICATION_STATUS_CHANGED,
                    "CoBrother Request " + (accepted ? "Accepted" : "Rejected"),
                    // Fixed: was missing a space before "was"
                    "Your CoBrother request for " + request.getEntityTitle() + " was " + outcome,
                    "/dashboard"
            );
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "status",  request.getStatus().name()
        ));
    }
}