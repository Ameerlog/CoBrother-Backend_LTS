package com.cobrother.web.service.admin;

import com.cobrother.web.Entity.cobranding.Domain;
import com.cobrother.web.Entity.cobranding.DomainEnquiry;
import com.cobrother.web.Entity.cobranding.DomainEnquiryStatus;
import com.cobrother.web.Entity.cobrother.CoBrotherRequest;
import com.cobrother.web.Entity.cobrother.CoBrotherRequestStatus;
import com.cobrother.web.Entity.cobrother.RequestType;
import com.cobrother.web.Entity.cocreation.Software;
import com.cobrother.web.Entity.cocreation.SoftwarePurchase;
import com.cobrother.web.Entity.cocreation.SoftwarePaymentStatus;
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
    @Autowired private SoftwarePurchaseRepository purchaseRepository;
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

    // ── Get all CoVenture applications ────────────────────────────────────────
    public ResponseEntity<?> getAllCoVentureApplications() {
        return ResponseEntity.ok(coVentureRepository.findAll());
    }

    // ── Get all domains (admin sees taken-down too) ───────────────────────────
    public ResponseEntity<?> getAllDomains() {
        return ResponseEntity.ok(domainRepository.findAll());
    }

    // ── Get all software listings (CoCreations tab) ───────────────────────────
    public ResponseEntity<?> getAllCoCreationPurchases() {
        return ResponseEntity.ok(softwareRepository.findAll());
    }

    // ── Get all CoBrother requests ────────────────────────────────────────────
    public ResponseEntity<?> getAllCoBrotherRequests() {
        return ResponseEntity.ok(coBrotherRequestRepository.findAllByOrderByCreatedAtDesc());
    }

    // ── Get all users with COBROTHER role ─────────────────────────────────────
    public ResponseEntity<?> getAllCoBrothers() {
        return ResponseEntity.ok(userRepository.findByRole(UserRole.COBROTHER));
    }

    // ── Get all domain enquiries ──────────────────────────────────────────────
    public ResponseEntity<?> getAllDomainEnquiries() {
        return ResponseEntity.ok(domainEnquiryRepository.findAllByOrderByCreatedAtDesc());
    }

    /**
     * CoCreation Requests tab.
     *
     * Returns every COMPLETED SoftwarePurchase as a plain Map so Jackson
     * serializes all fields eagerly — no Hibernate lazy-proxy nulls leak through.
     *
     * Every purchase card is always shown regardless of whether the CoBrother
     * addon has been added — the frontend uses `canForward` to decide whether
     * to enable the "Forward to CoBrother" button.
     *
     * Each item in the response array has this shape:
     * {
     *   purchaseId, soldAt, createdAt,
     *   coBrotherOptIn, coBrotherHelpPaid,
     *   canForward,      ← true only when addon is paid AND no active request exists
     *   addonStatus,     ← "NOT_ADDED" | "ADDED_NOT_PAID" | "PAID"
     *   paymentStatus, completionStatus,
     *   buyerFullName, buyerEmail, buyerPhone,
     *   buyer:    { id, firstname, lastname, email, phoneNumber },
     *   software: { id, name, price, category, techStack, purchaseType, purchaseCount,
     *               listedBy: { id, firstname, lastname, email, phoneNumber } },
     *   activeCoBrotherRequest: { id, status, assignedCoBrother: {...} } | null
     * }
     */
    public ResponseEntity<?> getAllCoCreationPurchaseRequests() {
        // Show ALL completed purchases — admin always sees buyer + software detail.
        // The addon state (coBrotherHelpPaid) only controls whether Forward is enabled.
        List<SoftwarePurchase> purchases =
                purchaseRepository.findByPaymentStatusOrderByCreatedAtDesc(
                        SoftwarePaymentStatus.COMPLETED);

        List<Map<String, Object>> result = new ArrayList<>();

        for (SoftwarePurchase p : purchases) {
            Map<String, Object> row = new LinkedHashMap<>();

            // ── Purchase scalars ──────────────────────────────────────────────
            row.put("purchaseId",        p.getId());
            row.put("soldAt",            p.getSoldAt());
            row.put("createdAt",         p.getCreatedAt());
            row.put("coBrotherOptIn",    p.isCoBrotherOptIn());
            row.put("coBrotherHelpPaid", p.isCoBrotherHelpPaid());

            // ── Addon status label (drives UI badge) ──────────────────────────
            // NOT_ADDED   → buyer never opted in
            // ADDED_NOT_PAID → opted in at purchase time but payment didn't complete (edge case)
            // PAID        → addon fully paid, Forward button should be enabled
            String addonStatus;
            if (p.isCoBrotherHelpPaid()) {
                addonStatus = "PAID";
            } else if (p.isCoBrotherOptIn()) {
                addonStatus = "ADDED_NOT_PAID";
            } else {
                addonStatus = "NOT_ADDED";
            }
            row.put("addonStatus", addonStatus);


            row.put("paymentStatus",     p.getPaymentStatus()    != null ? p.getPaymentStatus().name()    : null);
            row.put("razorpayOrderId",   p.getRazorpayOrderId());
            row.put("razorpayPaymentId", p.getRazorpayPaymentId());
            row.put("buyerFullName",     p.getBuyerFullName());
            row.put("buyerEmail",        p.getBuyerEmail());
            row.put("buyerPhone",        p.getBuyerPhone());

            // ── Buyer ─────────────────────────────────────────────────────────
            AppUser buyer = p.getBuyer();
            if (buyer != null) {
                Map<String, Object> b = new LinkedHashMap<>();
                b.put("id",          buyer.getId());
                b.put("firstname",   buyer.getFirstname());
                b.put("lastname",    buyer.getLastname());
                b.put("email",       buyer.getEmail());
                b.put("phoneNumber", buyer.getPhoneNumber());
                row.put("buyer", b);
            } else {
                row.put("buyer", null);
            }

            // ── Software ──────────────────────────────────────────────────────
            Software sw = p.getSoftware();
            if (sw != null) {
                Map<String, Object> s = new LinkedHashMap<>();
                s.put("id",           sw.getId());
                s.put("name",         sw.getName());
                s.put("price",        sw.getPrice());
                s.put("category",     sw.getCategory()     != null ? sw.getCategory().name()     : null);
                s.put("techStack",    sw.getTechStack());
                s.put("purchaseType", sw.getPurchaseType() != null ? sw.getPurchaseType().name() : null);
                s.put("purchaseCount",
                        purchaseRepository.countBySoftware_IdAndPaymentStatus(
                                sw.getId(), SoftwarePaymentStatus.COMPLETED));

                AppUser lister = sw.getListedBy();
                if (lister != null) {
                    Map<String, Object> l = new LinkedHashMap<>();
                    l.put("id",          lister.getId());
                    l.put("firstname",   lister.getFirstname());
                    l.put("lastname",    lister.getLastname());
                    l.put("email",       lister.getEmail());
                    l.put("phoneNumber", lister.getPhoneNumber());
                    s.put("listedBy", l);
                } else {
                    s.put("listedBy", null);
                }
                row.put("software", s);
            } else {
                row.put("software", null);
            }
// After:
            Optional<CoBrotherRequest> activeCbr = coBrotherRequestRepository
                    .findFirstByEntityIdAndRequestTypeAndStatusNotInOrderByCreatedAtDesc(
                            p.getId(),
                            RequestType.COCREATION,
                            List.of(CoBrotherRequestStatus.CANCELLED,
                                    CoBrotherRequestStatus.REJECTED));

// canForward = addon paid AND the existing request is still PENDING (no CoBrother assigned yet)
            boolean isPending = activeCbr.isPresent()
                    && activeCbr.get().getStatus() == CoBrotherRequestStatus.PENDING;
            row.put("canForward", p.isCoBrotherHelpPaid() && isPending);

            if (activeCbr.isPresent()) {
                CoBrotherRequest cbr = activeCbr.get();
                Map<String, Object> c = new LinkedHashMap<>();
                c.put("id",            cbr.getId());
                c.put("status",        cbr.getStatus().name());
                c.put("respondedAt",   cbr.getRespondedAt());
                c.put("coBrotherNote", cbr.getCoBrotherNote());

                AppUser cb = cbr.getAssignedCoBrother();
                if (cb != null) {
                    Map<String, Object> cbMap = new LinkedHashMap<>();
                    cbMap.put("id",        cb.getId());
                    cbMap.put("firstname", cb.getFirstname());
                    cbMap.put("lastname",  cb.getLastname());
                    cbMap.put("email",     cb.getEmail());
                    c.put("assignedCoBrother", cbMap);
                }
                row.put("activeCoBrotherRequest", c);
            } else {
                row.put("activeCoBrotherRequest", null);
            }

            result.add(row);
        }

        return ResponseEntity.ok(result);
    }

    // ── Forward a request to a CoBrother ─────────────────────────────────────
    public ResponseEntity<?> forwardToCoBrother(Long entityId, String type,
                                                Long coBrotherId, AppUser admin) {
        try {
            AppUser coBrother = userRepository.findById(coBrotherId)
                    .filter(u -> u.getRole() == UserRole.COBROTHER)
                    .orElse(null);
            if (coBrother == null)
                return ResponseEntity.badRequest().body(Map.of("error", "CoBrother not found"));

            RequestType requestType = RequestType.valueOf(type.toUpperCase());

            boolean alreadyAssignedToCoBrother = coBrotherRequestRepository
                    .existsByEntityIdAndRequestTypeAndAssignedCoBrother(
                            entityId, requestType, coBrother);
            if (alreadyAssignedToCoBrother)
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "This CoBrother has already been assigned to this request."));

            CoBrotherRequest request = new CoBrotherRequest();
            request.setRequestType(requestType);
            request.setEntityId(entityId);
            request.setAssignedCoBrother(coBrother);
            request.setCreatedByAdmin(admin);

            switch (requestType) {

                case COVENTURE -> {
                    CoVenture cv = coVentureRepository.findById(entityId)
                            .orElseThrow(() -> new RuntimeException("CoVenture not found"));
                    boolean activeExists = coBrotherRequestRepository
                            .existsByEntityIdAndRequestTypeAndStatusNotIn(entityId, requestType,
                                    List.of(CoBrotherRequestStatus.CANCELLED, CoBrotherRequestStatus.REJECTED));
                    if (activeExists)
                        return ResponseEntity.badRequest().body(Map.of(
                                "error", "An active CoBrother request already exists for this CoVenture."));
                    AppUser lister = cv.getVenture().getListedBy();
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
                    request.setStatus(CoBrotherRequestStatus.PAYMENT_PENDING);
                }

                case DOMAIN -> {
                    Domain domain = domainRepository.getDomainById(entityId);
                    if (domain == null) throw new RuntimeException("Domain not found");
                    boolean activeExists = coBrotherRequestRepository
                            .existsByEntityIdAndRequestTypeAndStatusNotIn(entityId, requestType,
                                    List.of(CoBrotherRequestStatus.CANCELLED, CoBrotherRequestStatus.REJECTED));
                    if (activeExists)
                        return ResponseEntity.badRequest().body(Map.of(
                                "error", "An active CoBrother request already exists for this Domain."));
                    AppUser lister = domain.getListedBy();
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
                    request.setStatus(CoBrotherRequestStatus.PAYMENT_PENDING);
                }

                case COCREATION -> {
                    SoftwarePurchase purchase = purchaseRepository.findById(entityId)
                            .orElseThrow(() -> new RuntimeException("SoftwarePurchase not found"));
                    if (purchase.getPaymentStatus() != SoftwarePaymentStatus.COMPLETED)
                        return ResponseEntity.badRequest().body(Map.of(
                                "error", "The buyer's software purchase is not yet completed."));
                    if (!purchase.isCoBrotherHelpPaid())
                        return ResponseEntity.badRequest().body(Map.of(
                                "error", "The buyer has not paid the CoBrother addon (₹1,000) yet."));

                    // Find the PENDING request auto-created at payment time
                    CoBrotherRequest existing = coBrotherRequestRepository
                            .findFirstByEntityIdAndRequestTypeAndStatus(
                                    entityId, RequestType.COCREATION, CoBrotherRequestStatus.PENDING)
                            .orElse(null);

                    if (existing == null)
                        return ResponseEntity.badRequest().body(Map.of(
                                "error", "No pending CoBrother request found for this purchase. " +
                                        "The addon may not have been paid yet."));

                    // Check this specific CoBrother isn't already assigned to a different request for this purchase
                    if (alreadyAssignedToCoBrother)
                        return ResponseEntity.badRequest().body(Map.of(
                                "error", "This CoBrother has already been assigned to this request."));

                    // Promote the PENDING record to FORWARDED with the chosen CoBrother
                    existing.setAssignedCoBrother(coBrother);
                    existing.setCreatedByAdmin(admin);
                    existing.setStatus(CoBrotherRequestStatus.FORWARDED);
                    existing.setPaidAt(purchase.getSoldAt() != null
                            ? purchase.getSoldAt() : LocalDateTime.now());

                    CoBrotherRequest saved = coBrotherRequestRepository.save(existing);

                    mailService.sendCoBrotherAssignmentEmail(
                            coBrother.getEmail(), coBrother.getFirstname(), saved);
                    notificationService.notify(coBrother,
                            com.cobrother.web.Entity.notification.NotificationType.COVENTURE_APPLICATION_RECEIVED,
                            "New Request Assigned",
                            "You have been assigned a CoBrother request for " + saved.getEntityTitle(),
                            "/cobrother");
                    return ResponseEntity.ok(Map.of("success", true, "requestId", saved.getId(),
                            "message", "CoBrother notified directly — buyer had already paid the addon."));
                }

                case DOMAIN_ENQUIRY -> {
                    DomainEnquiry enquiry = domainEnquiryRepository.findById(entityId)
                            .orElseThrow(() -> new RuntimeException("Enquiry not found"));
                    boolean activeExists = coBrotherRequestRepository
                            .existsByEntityIdAndRequestTypeAndStatusNotIn(entityId, requestType,
                                    List.of(CoBrotherRequestStatus.CANCELLED, CoBrotherRequestStatus.REJECTED));
                    if (activeExists)
                        return ResponseEntity.badRequest().body(Map.of(
                                "error", "An active CoBrother request already exists for this enquiry."));
                    AppUser lister = enquiry.getDomain().getListedBy();
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
                    request.setStatus(CoBrotherRequestStatus.PAYMENT_PENDING);
                    enquiry.setStatus(DomainEnquiryStatus.FORWARDED);
                    domainEnquiryRepository.save(enquiry);
                }
            }

            CoBrotherRequest saved = coBrotherRequestRepository.save(request);

            if (requestType == RequestType.COCREATION) {
                mailService.sendCoBrotherAssignmentEmail(
                        coBrother.getEmail(), coBrother.getFirstname(), saved);
                notificationService.notify(coBrother,
                        com.cobrother.web.Entity.notification.NotificationType.COVENTURE_APPLICATION_RECEIVED,
                        "New Request Assigned",
                        "You have been assigned a CoBrother request for " + saved.getEntityTitle(),
                        "/cobrother");
                return ResponseEntity.ok(Map.of("success", true, "requestId", saved.getId(),
                        "message", "CoBrother notified directly — buyer had already paid the addon."));
            }

            mailService.sendCoBrotherFeeRequestEmail(
                    request.getListerEmail(), request.getListerName(),
                    request.getEntityTitle(), saved.getId());
            return ResponseEntity.ok(Map.of("success", true, "requestId", saved.getId(),
                    "message", "Payment request sent to " + request.getListerName()));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed: " + e.getMessage()));
        }
    }

    // ── Lister creates ₹1,000 Razorpay order ─────────────────────────────────
    public ResponseEntity<?> createFeeOrder(Long requestId, AppUser lister) {
        try {
            CoBrotherRequest request = coBrotherRequestRepository.findById(requestId).orElse(null);
            if (request == null) return ResponseEntity.notFound().build();
            if (!request.getLister().getId().equals(lister.getId()))
                return ResponseEntity.status(403).body("Not your request");
            if (request.getStatus() != CoBrotherRequestStatus.PAYMENT_PENDING)
                return ResponseEntity.badRequest().body("Payment already processed");
            RazorpayClient client = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
            JSONObject orderReq = new JSONObject();
            orderReq.put("amount", 100000);
            orderReq.put("currency", "INR");
            orderReq.put("receipt", "cbr_" + requestId + "_" + lister.getId());
            Order order = client.orders.create(orderReq);
            request.setRazorpayOrderId(order.get("id").toString());
            coBrotherRequestRepository.save(request);
            return ResponseEntity.ok(Map.of(
                    "orderId", order.get("id").toString(), "amount", 1000,
                    "currency", "INR", "requestId", requestId, "keyId", razorpayKeyId));
        } catch (RazorpayException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Order creation failed: " + e.getMessage()));
        }
    }

    // ── Verify lister fee payment ─────────────────────────────────────────────
    public ResponseEntity<?> verifyFeePayment(Long requestId, String paymentId,
                                              String orderId, String signature, AppUser lister) {
        try {
            CoBrotherRequest request = coBrotherRequestRepository.findById(requestId).orElse(null);
            if (request == null) return ResponseEntity.notFound().build();
            if (!request.getLister().getId().equals(lister.getId()))
                return ResponseEntity.status(403).body("Not your request");
            String payload  = orderId + "|" + paymentId;
            String expected = hmacSHA256(payload, razorpayKeySecret);
            if (!expected.equals(signature))
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Payment verification failed"));
            request.setRazorpayPaymentId(paymentId);
            request.setPaidAt(LocalDateTime.now());
            request.setStatus(CoBrotherRequestStatus.FORWARDED);
            coBrotherRequestRepository.save(request);
            AppUser cb = request.getAssignedCoBrother();
            mailService.sendCoBrotherAssignmentEmail(cb.getEmail(), cb.getFirstname(), request);
            notificationService.notify(cb,
                    com.cobrother.web.Entity.notification.NotificationType.COVENTURE_APPLICATION_RECEIVED,
                    "New Request Assigned",
                    "You have been assigned a new " + request.getRequestType().name().toLowerCase() +
                            " request for " + request.getEntityTitle(), "/cobrother");
            return ResponseEntity.ok(Map.of("success", true, "message", "Payment verified, CoBrother notified"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // ── Lister cancels a PAYMENT_PENDING request ──────────────────────────────
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

    // ── Get pending fee requests visible to the lister ────────────────────────
    public ResponseEntity<?> getMyFeeRequests(AppUser lister) {
        return ResponseEntity.ok(coBrotherRequestRepository.findByLister(lister));
    }

    // ── Take down a listing ───────────────────────────────────────────────────
    public ResponseEntity<?> takeDownListing(String type, Long entityId, String reason) {
        try {
            switch (type.toUpperCase()) {
                case "VENTURE" -> {
                    Venture v = ventureRepository.findById(entityId)
                            .orElseThrow(() -> new RuntimeException("Not found"));
                    v.setTakenDown(true); v.setTakeDownReason(reason); v.setStatus(false);
                    ventureRepository.save(v);
                }
                case "DOMAIN" -> {
                    Domain d = domainRepository.getDomainById(entityId);
                    if (d == null) throw new RuntimeException("Not found");
                    d.setTakenDown(true); d.setTakeDownReason(reason); d.setStatus(false);
                    domainRepository.save(d);
                }
                case "SOFTWARE" -> {
                    Software s = softwareRepository.findSoftwareById(entityId);
                    if (s == null) throw new RuntimeException("Not found");
                    s.setTakenDown(true); s.setTakeDownReason(reason); s.setStatus(false);
                    softwareRepository.save(s);
                }
                default -> { return ResponseEntity.badRequest().body("Invalid type"); }
            }
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
                    v.setTakenDown(false); v.setTakeDownReason(null); v.setStatus(true);
                    ventureRepository.save(v);
                }
                case "DOMAIN" -> {
                    Domain d = domainRepository.getDomainById(entityId);
                    if (d == null) throw new RuntimeException("Not found");
                    d.setTakenDown(false); d.setTakeDownReason(null); d.setStatus(true);
                    domainRepository.save(d);
                }
                case "SOFTWARE" -> {
                    Software s = softwareRepository.findSoftwareById(entityId);
                    if (s == null) throw new RuntimeException("Not found");
                    s.setTakenDown(false); s.setTakeDownReason(null); s.setStatus(true);
                    softwareRepository.save(s);
                }
            }
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
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

    private void logAdminAction(String action, String detail) {
        System.out.println("[ADMIN ACTION] " + action + ": " + detail);
    }
}