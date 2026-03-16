package com.cobrother.web.service.admin;

import com.cobrother.web.Entity.cobranding.Domain;
import com.cobrother.web.Entity.cobranding.DomainEnquiry;
import com.cobrother.web.Entity.cobranding.DomainEnquiryStatus;
import com.cobrother.web.Entity.cobrother.CoBrotherRequest;
import com.cobrother.web.Entity.cobrother.CoBrotherRequestStatus;
import com.cobrother.web.Entity.cobrother.RequestType;
import com.cobrother.web.Entity.cocreation.Software;
import com.cobrother.web.Entity.coventure.CoVenture;
import com.cobrother.web.Entity.coventure.Venture;
import com.cobrother.web.Entity.user.AppUser;
import com.cobrother.web.Entity.user.UserRole;
import com.cobrother.web.Repository.*;
import com.cobrother.web.service.auth.CurrentUserService;
import com.cobrother.web.service.auth.MailService;
import com.cobrother.web.service.notification.NotificationService;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class AdminService {

    @Autowired private CoBrotherRequestRepository coBrotherRequestRepository;
    @Autowired private CoVentureRepository coVentureRepository;
    @Autowired private DomainRepository domainRepository;
    @Autowired private SoftwareRepository softwareRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private CurrentUserService currentUserService;
    @Autowired private MailService mailService;
    @Autowired private NotificationService notificationService;
    @Autowired private DomainEnquiryRepository domainEnquiryRepository;
    @Autowired private VentureRepository ventureRepository;

    @Value("${razorpay.key-id}")
    private String razorpayKeyId;

    @Value("${razorpay.key-secret}")
    private String razorpayKeySecret;

    // ── Get all requests by type for admin view ───────────────────────────────

    public ResponseEntity<?> getAllCoVentureApplications() {
        return ResponseEntity.ok(coVentureRepository.findAll());
    }

    public ResponseEntity<?> getAllDomainPurchases() {
        return ResponseEntity.ok(domainRepository.findAll());
    }

    public ResponseEntity<?> getAllCoCreationPurchases() {
        return ResponseEntity.ok(softwareRepository.findAll());
    }

    public ResponseEntity<?> getAllCoBrotherRequests() {
        return ResponseEntity.ok(coBrotherRequestRepository.findAllByOrderByCreatedAtDesc());
    }

    public ResponseEntity<?> getAllCoBrothers() {
        return ResponseEntity.ok(userRepository.findByRole(UserRole.COBROTHER));
    }

    public ResponseEntity<?> getAllDomains() {
        // Admin sees ALL domains including taken down — not just status=true
        return ResponseEntity.ok(domainRepository.findAll());
    }


    // ── Forward request to CoBrother ─────────────────────────────────────────
    public ResponseEntity<?> forwardToCoBrother(Long entityId, String type,
                                                Long coBrotherId, AppUser admin) {
        try {
            UserRole role = UserRole.COBROTHER;
            AppUser coBrother = userRepository.findById(coBrotherId)
                    .filter(u -> u.getRole() == role)
                    .orElse(null);
            if (coBrother == null)
                return ResponseEntity.badRequest().body(Map.of("error", "CoBrother not found"));

            RequestType requestType = RequestType.valueOf(type.toUpperCase());

            // ── 1. Check if this exact CoBrother already has a request for this entity ──
            boolean alreadyAssignedToCoBrother = coBrotherRequestRepository
                    .existsByEntityIdAndRequestTypeAndAssignedCoBrother(
                            entityId, requestType, coBrother);
            if (alreadyAssignedToCoBrother) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "This CoBrother has already been assigned to this request."
                ));
            }

            // ── 2. Check if lister already has an active (non-cancelled) payment request ──
            boolean listerAlreadyHasActiveRequest = false;
            // We need lister info first — resolve entity to get lister
            AppUser lister = resolveLister(entityId, requestType);
            if (lister != null) {
                listerAlreadyHasActiveRequest = coBrotherRequestRepository
                        .existsByEntityIdAndRequestTypeAndListerAndStatusNot(
                                entityId, requestType, lister, CoBrotherRequestStatus.CANCELLED);
            }

            if (listerAlreadyHasActiveRequest) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "The lister already has an active payment request for this entity. " +
                                "Wait for them to complete or cancel it before assigning another CoBrother."
                ));
            }

            CoBrotherRequest request = new CoBrotherRequest();
            request.setRequestType(requestType);
            request.setEntityId(entityId);
            request.setAssignedCoBrother(coBrother);
            request.setCreatedByAdmin(admin);
            request.setStatus(CoBrotherRequestStatus.PAYMENT_PENDING);

            // Snapshot details based on type
            switch (requestType) {
                case COVENTURE -> {
                    CoVenture cv = coVentureRepository.findById(entityId)
                            .orElseThrow(() -> new RuntimeException("CoVenture not found"));
                    lister = cv.getVenture().getListedBy();
                    AppUser applicant = cv.getApplicant();
                    request.setLister(lister);
                    request.setListerName(lister.getFirstname() + " " + lister.getLastname());
                    request.setListerEmail(lister.getEmail());
                    request.setListerPhone(lister.getPhoneNumber());
                    request.setApplicantName(cv.getFullName());
                    request.setApplicantEmail(applicant.getEmail());
                    request.setApplicantPhone(cv.getPhone());
                    request.setEntityTitle(cv.getVenture().getBrandDetails().getBrandName());
                    request.setEntityDetails("{\"type\":\"CoVenture\",\"description\":\"" +
                            cv.getVenture().getBrandDetails().getDescription() + "\"}");
                }
                case DOMAIN -> {
                    Domain domain = domainRepository.getDomainById(entityId);
                    if (domain == null) throw new RuntimeException("Domain not found");
                    lister = domain.getListedBy();
                    AppUser buyer  = domain.getPurchasedBy();
                    request.setLister(lister);
                    request.setListerName(lister.getFirstname() + " " + lister.getLastname());
                    request.setListerEmail(lister.getEmail());
                    request.setListerPhone(lister.getPhoneNumber());
                    if (buyer != null) {
                        request.setApplicantName(buyer.getFirstname() + " " + buyer.getLastname());
                        request.setApplicantEmail(buyer.getEmail());
                        request.setApplicantPhone(buyer.getPhoneNumber());
                    }
                    request.setEntityTitle(domain.getDomainName() + domain.getDomainExtension());
                    request.setEntityDetails("{\"type\":\"Domain\",\"price\":" + domain.getAskingPrice() + "}");
                }
                case COCREATION -> {
                    Software sw = softwareRepository.findSoftwareById(entityId);
                    if (sw == null) throw new RuntimeException("Software not found");
                    lister = sw.getListedBy();
                    AppUser buyer  = sw.getPurchasedBy();
                    request.setLister(lister);
                    request.setListerName(lister.getFirstname() + " " + lister.getLastname());
                    request.setListerEmail(lister.getEmail());
                    request.setListerPhone(lister.getPhoneNumber());
                    if (buyer != null) {
                        request.setApplicantName(buyer.getFirstname() + " " + buyer.getLastname());
                        request.setApplicantEmail(buyer.getEmail());
                        request.setApplicantPhone(buyer.getPhoneNumber());
                    }
                    request.setEntityTitle(sw.getName());
                    request.setEntityDetails("{\"type\":\"Software\",\"price\":" + sw.getPrice() + "}");
                }
                // Add new case in the RequestType switch:
                case DOMAIN_ENQUIRY -> {
                    DomainEnquiry enquiry = domainEnquiryRepository.findById(entityId)
                            .orElseThrow(() -> new RuntimeException("Enquiry not found"));
                    lister = enquiry.getDomain().getListedBy();
                    AppUser enquirer = enquiry.getEnquirer();

                    request.setLister(lister);
                    request.setListerName(lister.getFirstname() + " " + lister.getLastname());
                    request.setListerEmail(lister.getEmail());
                    request.setListerPhone(lister.getPhoneNumber());
                    request.setApplicantName(enquiry.getFullName());
                    request.setApplicantEmail(enquiry.getEmail());
                    request.setApplicantPhone(enquiry.getPhone());
                    request.setEntityTitle(enquiry.getDomain().getDomainName() +
                            enquiry.getDomain().getDomainExtension());
                    request.setEntityDetails("{\"type\":\"DomainEnquiry\",\"message\":\"" +
                            enquiry.getMessage() + "\",\"price\":" +
                            enquiry.getDomain().getAskingPrice() + "}");

                    // Mark enquiry as forwarded
                    enquiry.setStatus(DomainEnquiryStatus.FORWARDED);
                    domainEnquiryRepository.save(enquiry);
                }
            }

            CoBrotherRequest saved = coBrotherRequestRepository.save(request);

            // Send payment request email to lister
            mailService.sendCoBrotherFeeRequestEmail(
                    request.getListerEmail(),
                    request.getListerName(),
                    request.getEntityTitle(),
                    saved.getId()
            );

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "requestId", saved.getId(),
                    "message", "Payment request sent to " + request.getListerName()
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed: " + e.getMessage()));
        }
    }
    private AppUser resolveLister(Long entityId, RequestType requestType) {
        return switch (requestType) {
            case COVENTURE -> coVentureRepository.findById(entityId)
                    .map(cv -> cv.getVenture().getListedBy()).orElse(null);
            case DOMAIN -> {
                Domain d = domainRepository.getDomainById(entityId);
                yield d != null ? d.getListedBy() : null;
            }
            case COCREATION -> {
                Software s = softwareRepository.findSoftwareById(entityId);
                yield s != null ? s.getListedBy() : null;
            }
            case DOMAIN_ENQUIRY -> {                          // ✅ add this case
                DomainEnquiry enquiry = domainEnquiryRepository
                        .findById(entityId).orElse(null);
                yield enquiry != null
                        ? enquiry.getDomain().getListedBy()
                        : null;
            }
        };
    }


    // ── Lister creates ₹1000 Razorpay order ──────────────────────────────────
    public ResponseEntity<?> createFeeOrder(Long requestId, AppUser lister) {
        try {
            CoBrotherRequest request = coBrotherRequestRepository.findById(requestId)
                    .orElse(null);
            if (request == null) return ResponseEntity.notFound().build();
            if (!request.getLister().getId().equals(lister.getId()))
                return ResponseEntity.status(403).body("Not your request");
            if (request.getStatus() != CoBrotherRequestStatus.PAYMENT_PENDING)
                return ResponseEntity.badRequest().body("Payment already processed");

            RazorpayClient client = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
            JSONObject orderReq = new JSONObject();
            orderReq.put("amount", 100000); // ₹1000 in paise
            orderReq.put("currency", "INR");
            orderReq.put("receipt", "cbr_" + requestId + "_" + lister.getId());

            Order order = client.orders.create(orderReq);
            request.setRazorpayOrderId(order.get("id").toString());
            coBrotherRequestRepository.save(request);

            return ResponseEntity.ok(Map.of(
                    "orderId", order.get("id").toString(),
                    "amount", 1000,
                    "currency", "INR",
                    "requestId", requestId,
                    "keyId", razorpayKeyId
            ));
        } catch (RazorpayException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Order creation failed: " + e.getMessage()));
        }
    }

    // ── Verify lister fee payment ─────────────────────────────────────────────
    public ResponseEntity<?> verifyFeePayment(Long requestId, String paymentId,
                                              String orderId, String signature,
                                              AppUser lister) {
        try {
            CoBrotherRequest request = coBrotherRequestRepository.findById(requestId)
                    .orElse(null);
            if (request == null) return ResponseEntity.notFound().build();
            if (!request.getLister().getId().equals(lister.getId()))
                return ResponseEntity.status(403).body("Not your request");

            String payload  = orderId + "|" + paymentId;
            String expected = hmacSHA256(payload, razorpayKeySecret);

            if (!expected.equals(signature)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Payment verification failed"));
            }

            request.setRazorpayPaymentId(paymentId);
            request.setPaidAt(LocalDateTime.now());
            request.setStatus(CoBrotherRequestStatus.FORWARDED);
            coBrotherRequestRepository.save(request);

            // Notify CoBrother via email + notification
            AppUser coBrother = request.getAssignedCoBrother();
            mailService.sendCoBrotherAssignmentEmail(
                    coBrother.getEmail(),
                    coBrother.getFirstname(),
                    request
            );
            notificationService.notify(
                    coBrother,
                    com.cobrother.web.Entity.notification.NotificationType.COVENTURE_APPLICATION_RECEIVED,
                    "New Request Assigned",
                    "You have been assigned a new " + request.getRequestType().name().toLowerCase() +
                            " request for "+ request.getEntityTitle(),
                    "/cobrother"
            );

            return ResponseEntity.ok(Map.of("success", true, "message", "Payment verified, CoBrother notified"));

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ── Lister cancels payment request ────────────────────────────────────────
    public ResponseEntity<?> cancelFeeRequest(Long requestId, AppUser lister) {
        CoBrotherRequest request = coBrotherRequestRepository.findById(requestId).orElse(null);
        if (request == null) return ResponseEntity.notFound().build();
        if (!request.getLister().getId().equals(lister.getId()))
            return ResponseEntity.status(403).body("Not your request");
        if (request.getStatus() != CoBrotherRequestStatus.PAYMENT_PENDING)
            return ResponseEntity.badRequest().body("Cannot cancel at this stage");
        request.setStatus(CoBrotherRequestStatus.CANCELLED);
        coBrotherRequestRepository.save(request);
        return ResponseEntity.ok(Map.of("success", true));
    }

    // ── Get pending fee requests for lister ───────────────────────────────────
    public ResponseEntity<?> getMyFeeRequests(AppUser lister) {
        return ResponseEntity.ok(coBrotherRequestRepository.findByLister(lister));
    }

    private String hmacSHA256(String data, String key) throws Exception {
        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
        javax.crypto.spec.SecretKeySpec spec =
                new javax.crypto.spec.SecretKeySpec(key.getBytes("UTF-8"), "HmacSHA256");
        mac.init(spec);
        byte[] hash = mac.doFinal(data.getBytes("UTF-8"));
        StringBuilder hex = new StringBuilder();
        for (byte b : hash) hex.append(String.format("%02x", b));
        return hex.toString();
    }

    public ResponseEntity<?> getAllDomainEnquiries() {
        return ResponseEntity.ok(domainEnquiryRepository.findAllByOrderByCreatedAtDesc());
    }

    // ── Take down a listing ───────────────────────────────────────────────────
    public ResponseEntity<?> takeDownListing(String type, Long entityId, String reason) {
        try {
            switch (type.toUpperCase()) {
                case "VENTURE" -> {
                    Venture v = ventureRepository.findById(entityId)
                            .orElseThrow(() -> new RuntimeException("Not found"));
                    v.setTakenDown(true);
                    v.setTakeDownReason(reason);
                    v.setStatus(false); // hide from public listings
                    ventureRepository.save(v);
                }
                case "DOMAIN" -> {
                    Domain d = domainRepository.getDomainById(entityId);
                    if (d == null) throw new RuntimeException("Not found");
                    d.setTakenDown(true);
                    d.setTakeDownReason(reason);
                    d.setStatus(false);
                    domainRepository.save(d);
                }
                case "SOFTWARE" -> {
                    Software s = softwareRepository.findSoftwareById(entityId);
                    if (s == null) throw new RuntimeException("Not found");
                    s.setTakenDown(true);
                    s.setTakeDownReason(reason);
                    s.setStatus(false);
                    softwareRepository.save(s);
                }
                default -> { return ResponseEntity.badRequest().body("Invalid type"); }
            }

            // Log audit trail
            logAdminAction("TAKEDOWN", type + " #" + entityId + " — " + reason);

            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // ── Restore a taken-down listing ──────────────────────────────────────────
    public ResponseEntity<?> restoreListing(String type, Long entityId) {
        try {
            switch (type.toUpperCase()) {
                case "VENTURE" -> {
                    Venture v = ventureRepository.findById(entityId)
                            .orElseThrow(() -> new RuntimeException("Not found"));
                    v.setTakenDown(false);
                    v.setTakeDownReason(null);
                    v.setStatus(true);
                    ventureRepository.save(v);
                }
                case "DOMAIN" -> {
                    Domain d = domainRepository.getDomainById(entityId);
                    if (d == null) throw new RuntimeException("Not found");
                    d.setTakenDown(false);
                    d.setTakeDownReason(null);
                    d.setStatus(true);
                    domainRepository.save(d);
                }
                case "SOFTWARE" -> {
                    Software s = softwareRepository.findSoftwareById(entityId);
                    if (s == null) throw new RuntimeException("Not found");
                    s.setTakenDown(false);
                    s.setTakeDownReason(null);
                    s.setStatus(true);
                    softwareRepository.save(s);
                }
            }
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // Placeholder — wire to AuditLog when that's built
    private void logAdminAction(String action, String detail) {
        System.out.println("[ADMIN ACTION] " + action + ": " + detail);
    }
}
