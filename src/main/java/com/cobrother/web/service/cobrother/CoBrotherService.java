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

    public ResponseEntity<?> getMyRequests(AppUser coBrother) {
        List<CoBrotherRequest> requests =
                requestRepository.findByAssignedCoBrotherOrderByCreatedAtDesc(coBrother);
        return ResponseEntity.ok(requests);
    }

    public ResponseEntity<?> respond(Long requestId, boolean accepted,
                                     String note, AppUser coBrother) {
        CoBrotherRequest request = requestRepository.findById(requestId).orElse(null);
        if (request == null) return ResponseEntity.notFound().build();
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

        // ── If accepted, cancel all other FORWARDED/PAYMENT_PENDING requests
        //    for the same entity so lister doesn't get multiple accepted requests ──
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
                // Notify the other CoBrothers their request was auto-cancelled
                notificationService.notify(
                        r.getAssignedCoBrother(),
                        com.cobrother.web.Entity.notification.NotificationType
                                .COVENTURE_APPLICATION_STATUS_CHANGED,
                        "Request Closed",
                        "The request for " + r.getEntityTitle() +
                        " has been handled by another CoBrother.",
                        "/cobrother"
                );
            });
        }
        // Notify lister
        String outcome = accepted ? "accepted" : "rejected";
        mailService.sendCoBrotherResponseEmail(
                request.getListerEmail(),
                request.getListerName(),
                request.getEntityTitle(),
                accepted,
                note
        );

        // Notify via in-app notification to lister
        if (request.getLister() != null) {
            notificationService.notify(
                    request.getLister(),
                    NotificationType.COVENTURE_APPLICATION_STATUS_CHANGED,
                    "CoBrother Request " + (accepted ? "Accepted" : "Rejected"),
                    "Your CoBrother request for " + request.getEntityTitle() + "was " + outcome,
                    "/dashboard"
            );
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "status", request.getStatus().name()
        ));
    }
}
