package com.cobrother.web.service.cocreation;

import com.cobrother.web.Entity.cobrother.CoBrotherRequest;
import com.cobrother.web.Entity.cobrother.CoBrotherRequestStatus;
import com.cobrother.web.Entity.cobrother.RequestType;
import com.cobrother.web.Entity.cocreation.*;
import com.cobrother.web.Entity.user.AppUser;
import com.cobrother.web.Repository.*;
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
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SoftwareService {

    @Autowired private SoftwareRepository softwareRepository;
    @Autowired private SoftwareViewRepository softwareViewRepository;
    @Autowired private CommunityRepository communityRepository;
    @Autowired private MailService mailService;
    @Autowired private SoftwarePurchaseRepository purchaseRepository;

    @Value("${razorpay.key-id}")
    private String razorpayKeyId;

    @Value("${razorpay.key-secret}")
    private String razorpayKeySecret;

    @Autowired
    private NotificationService notificationService;

    @Autowired private CoBrotherRequestRepository coBrotherRequestRepository;

    // ── CRUD ──────────────────────────────────────────────────────────────────

    public ResponseEntity<List<Software>> getAll() {
        List<Software> list = softwareRepository.findByStatusTrue();
        list.forEach(s -> s.setPurchaseCount(
                purchaseRepository.countBySoftware_IdAndPaymentStatus(
                        s.getId(), SoftwarePaymentStatus.COMPLETED)));
        return ResponseEntity.ok(list);
    }

    public ResponseEntity<Software> getById(Long id, AppUser viewer) {
        Software software = softwareRepository.findSoftwareById(id);
        if (software == null) return ResponseEntity.notFound().build();

        // Track view
        trackView(software, viewer);


        return ResponseEntity.ok(software);
    }

    public ResponseEntity<List<Software>> getMyListings(AppUser user) {
        List<Software> list = softwareRepository.findByListedBy(user);
        list.forEach(s -> s.setPurchaseCount(
                purchaseRepository.countBySoftware_IdAndPaymentStatus(
                        s.getId(), SoftwarePaymentStatus.COMPLETED)));
        return ResponseEntity.ok(list);
    }
    public ResponseEntity<List<SoftwarePurchase>> getMyPurchases(AppUser buyer) {
        List<SoftwarePurchase> purchases = purchaseRepository.findByBuyer(buyer);
        // For each completed purchase, include github link
        purchases.forEach(p -> {
            if (p.getPaymentStatus() != SoftwarePaymentStatus.COMPLETED) {
                p.getSoftware().setGithubLink(null);
            }
        });
        return ResponseEntity.ok(purchases);
    }


    public ResponseEntity<Software> create(Software software) {
        try {
            software.setStatus(true);
            software.setSoftwareStatus(SoftwareStatus.AVAILABLE);
            // pricingDemand is no longer collected from the form — default to FIXED
            if (software.getPricingDemand() == null) {
                software.setPricingDemand(SoftwarePricingDemand.FIXED);
            }
            return ResponseEntity.ok(softwareRepository.save(software));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    public ResponseEntity<Software> update(Long id, Software incoming, AppUser user) {
        try {
            Software existing = softwareRepository.findSoftwareById(id);
            if (existing == null) return ResponseEntity.notFound().build();
            if (!existing.getListedBy().getId().equals(user.getId()))
                return ResponseEntity.status(403).build();

            existing.setName(incoming.getName());
            existing.setDescription(incoming.getDescription());
            existing.setVideoLink(incoming.getVideoLink());
            existing.setWhatItDoes(incoming.getWhatItDoes());
            existing.setHowItHelps(incoming.getHowItHelps());
            existing.setGithubLink(incoming.getGithubLink());
            existing.setLiveDemoLink(incoming.getLiveDemoLink());
            existing.setTechStack(incoming.getTechStack());
            existing.setCategory(incoming.getCategory());
            existing.setPricingDemand(incoming.getPricingDemand());
            existing.setPrice(incoming.getPrice());
            return ResponseEntity.ok(softwareRepository.save(existing));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    public ResponseEntity<Software> delete(Long id, AppUser user) {
        try {
            Software software = softwareRepository.findSoftwareById(id);
            if (software == null) return ResponseEntity.notFound().build();
            if (!software.getListedBy().getId().equals(user.getId()))
                return ResponseEntity.status(403).build();
            software.setStatus(false);
            return ResponseEntity.ok(softwareRepository.save(software));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    // ── Payment ───────────────────────────────────────────────────────────────

    public ResponseEntity<?> createOrder(Long id, AppUser buyer,
                                         String buyerFullName, String buyerEmail,
                                         String buyerPhone, boolean coBrotherOptIn) {
        try {
            Software software = softwareRepository.findSoftwareById(id);
            if (software == null) return ResponseEntity.notFound().build();
            if (software.getListedBy().getId().equals(buyer.getId()))
                return ResponseEntity.badRequest().body("You cannot buy your own listing");

            // Check if this buyer already has a completed purchase for this software
            Optional<SoftwarePurchase> existingPurchase =
                    purchaseRepository.findBySoftware_IdAndBuyer(id, buyer);
            if (existingPurchase.isPresent() &&
                    existingPurchase.get().getPaymentStatus() == SoftwarePaymentStatus.COMPLETED) {
                return ResponseEntity.badRequest().body("You have already purchased this software");
            }

            double basePrice    = software.getPrice();
            double coBrotherFee = coBrotherOptIn ? 1000.0 : 0.0;
            double totalAmount  = basePrice + coBrotherFee;

            RazorpayClient client = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", (int)(totalAmount * 100));
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "sw_" + id + "_" + buyer.getId());

            Order order = client.orders.create(orderRequest);

            // Create SoftwarePurchase record
            SoftwarePurchase purchase = new SoftwarePurchase();
            purchase.setSoftware(software);
            purchase.setBuyer(buyer);
            purchase.setBuyerFullName(buyerFullName);
            purchase.setBuyerEmail(buyerEmail);
            purchase.setBuyerPhone(buyerPhone);
            purchase.setRazorpayOrderId(order.get("id").toString());
            purchase.setPaymentStatus(SoftwarePaymentStatus.CREATED);
            purchase.setCoBrotherOptIn(coBrotherOptIn);
            purchaseRepository.save(purchase);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("orderId",        order.get("id").toString());
            response.put("amount",         totalAmount);
            response.put("basePrice",      basePrice);
            response.put("coBrotherFee",   coBrotherFee);
            response.put("coBrotherOptIn", coBrotherOptIn);
            response.put("currency",       "INR");
            response.put("softwareId",     id);
            response.put("keyId",          razorpayKeyId);
            return ResponseEntity.ok(response);

        } catch (RazorpayException e) {
            return ResponseEntity.internalServerError().body("Order creation failed: " + e.getMessage());
        }
    }

    // ── verifyPayment ────────────────────────────────────────────────────────────
    public ResponseEntity<?> verifyPayment(Long softwareId, String paymentId,
                                           String orderId, String signature,
                                           AppUser buyer) {
        try {
            SoftwarePurchase purchase = purchaseRepository.findByRazorpayOrderId(orderId)
                    .orElse(null);
            if (purchase == null) return ResponseEntity.notFound().build();

            String payload  = orderId + "|" + paymentId;
            String expected = hmacSHA256(payload, razorpayKeySecret);

            if (!expected.equals(signature)) {
                purchase.setPaymentStatus(SoftwarePaymentStatus.FAILED);
                purchaseRepository.save(purchase);
                return ResponseEntity.badRequest().body(Map.of("success", false,
                        "message", "Verification failed"));
            }

            Software software = purchase.getSoftware();
            purchase.setPaymentStatus(SoftwarePaymentStatus.COMPLETED);
            purchase.setRazorpayPaymentId(paymentId);
            purchase.setSoldAt(LocalDateTime.now());

            if (purchase.isCoBrotherOptIn()) {
                purchase.setCoBrotherHelpPaid(true);
            }
            purchaseRepository.save(purchase);

            // Auto-create CoBrotherRequest so admin can forward immediately
            // Always create a CoBrother request so admin can track the purchase.
            // The request captures whether the addon was paid; Forward button is
            // shown only when coBrotherHelpPaid == true.
            createCoCrationRequest(purchase, software);

            software.setViews(software.getViews() + 1);
            softwareRepository.save(software);

            mailService.sendSoftwarePurchaseBuyerEmail(
                    buyer.getEmail(), buyer.getFirstname(),
                    software.getName(), software.getPrice(), paymentId, software.getGithubLink());
            mailService.sendSoftwarePurchaseSellerEmail(
                    software.getListedBy().getEmail(), software.getListedBy().getFirstname(),
                    software.getName(), purchase.getBuyerFullName(), software.getPrice());

            notificationService.notifySoftwarePurchased(
                    software.getListedBy(), software.getName(),
                    buyer.getFirstname() + " " + buyer.getLastname());

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success",    true);
            response.put("message",    "Payment successful");
            response.put("githubLink", software.getGithubLink() != null ? software.getGithubLink() : "");
            response.put("purchaseId", purchase.getId());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    // ── handleFailure ────────────────────────────────────────────────────────────
    public ResponseEntity<?> handleFailure(Long softwareId, AppUser buyer) {
        try {
            // Find the most recent CREATED purchase for this buyer+software
            purchaseRepository.findBySoftware_IdAndBuyer(softwareId, buyer)
                    .ifPresent(p -> {
                        if (p.getPaymentStatus() == SoftwarePaymentStatus.CREATED) {
                            p.setPaymentStatus(SoftwarePaymentStatus.FAILED);
                            purchaseRepository.save(p);
                        }
                    });
            return ResponseEntity.ok(Map.of("success", false));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // ── Analytics ─────────────────────────────────────────────────────────────
    public ResponseEntity<Map<String, Object>> getAnalytics(Long softwareId, AppUser user) {
        Software software = softwareRepository.findSoftwareById(softwareId);
        if (software == null) return ResponseEntity.notFound().build();
        if (!software.getListedBy().getId().equals(user.getId()))
            return ResponseEntity.status(403).build();

        List<SoftwareView> views = softwareViewRepository.findBySoftware_Id(softwareId);

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM dd");
        Map<String, Long> viewsByDay = new LinkedHashMap<>();
        LocalDateTime now = LocalDateTime.now();
        for (int i = 29; i >= 0; i--) viewsByDay.put(now.minusDays(i).format(fmt), 0L);
        views.forEach(v -> {
            String key = v.getViewedAt().format(fmt);
            if (viewsByDay.containsKey(key)) viewsByDay.merge(key, 1L, Long::sum);
        });

        Map<String, Long> byIndustry = views.stream()
                .filter(v -> v.getViewerIndustry() != null)
                .collect(Collectors.groupingBy(SoftwareView::getViewerIndustry, Collectors.counting()));

        Map<String, Long> byRole = views.stream()
                .filter(v -> v.getViewerRole() != null)
                .collect(Collectors.groupingBy(SoftwareView::getViewerRole, Collectors.counting()));

        // Revenue from confirmed purchases
        List<SoftwarePurchase> allPurchases = purchaseRepository.findBySoftware_Id(softwareId);
        long totalSales = allPurchases.stream()
                .filter(p -> p.getPaymentStatus() == SoftwarePaymentStatus.COMPLETED).count();
        double totalRevenue = totalSales * software.getPrice();

// Replace totalRevenue/totalSales section in result map:

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("softwareId", softwareId);
        result.put("softwareName", software.getName());
        result.put("totalViews", views.size());
        result.put("totalSales", totalSales);
        result.put("totalRevenue", totalRevenue);
        result.put("purchasers", allPurchases.stream()
                .filter(p -> p.getPaymentStatus() == SoftwarePaymentStatus.COMPLETED)
                .map(p -> Map.of(
                        "buyerName", p.getBuyerFullName() != null ? p.getBuyerFullName() : "",
                        "soldAt",    p.getSoldAt() != null ? p.getSoldAt().toString() : "")).collect(Collectors.toList()));
        result.put("viewsByDay", viewsByDay);
        result.put("byIndustry", byIndustry);
        result.put("byRole", byRole);
        return ResponseEntity.ok(result);
    }

    // ── View tracking ─────────────────────────────────────────────────────────
    private void trackView(Software software, AppUser viewer) {
        if (viewer != null && viewer.getId().equals(software.getListedBy().getId())) return;

        SoftwareView view = new SoftwareView();
        view.setSoftware(software);
        view.setViewer(viewer);

        if (viewer != null) {
            communityRepository.findByAppUser(viewer).ifPresent(c -> {
                if (c.getIndustry() != null) view.setViewerIndustry(c.getIndustry().name());
                if (c.getRole() != null) view.setViewerRole(c.getRole().name());
            });
        }

        softwareViewRepository.save(view);
        software.setViews(software.getViews() + 1);
        softwareRepository.save(software);
    }

    private String hmacSHA256(String data, String key) throws Exception {
        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
        javax.crypto.spec.SecretKeySpec secretKeySpec =
                new javax.crypto.spec.SecretKeySpec(key.getBytes("UTF-8"), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] hash = mac.doFinal(data.getBytes("UTF-8"));
        StringBuilder hex = new StringBuilder();
        for (byte b : hash) hex.append(String.format("%02x", b));
        return hex.toString();
    }


    public ResponseEntity<?> createPurchaseOrder(long softwareId, AppUser buyer,
                                                 boolean coBrotherOptIn) {
        try {
            Software software = softwareRepository.findSoftwareById(softwareId);
            if (software == null) return ResponseEntity.notFound().build();
            if (software.getListedBy().getId().equals(buyer.getId()))
                return ResponseEntity.badRequest().body("You cannot buy your own software");
            if (software.getSoftwareStatus() != SoftwareStatus.AVAILABLE)
                return ResponseEntity.badRequest().body("Software is not available");

            double basePrice     = software.getPrice();
            double coBrotherFee  = coBrotherOptIn ? 1000.0 : 0.0;
            double totalAmount   = basePrice + coBrotherFee;

            RazorpayClient client = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", (int)(totalAmount * 100)); // paise
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "sw_" + softwareId + "_" + buyer.getId());

            Order order = client.orders.create(orderRequest);

            software.setSoftwareStatus(SoftwareStatus.PENDING);
            softwareRepository.save(software);

            return ResponseEntity.ok(Map.of(
                    "orderId",       order.get("id").toString(),
                    "amount",        totalAmount,
                    "basePrice",     basePrice,
                    "coBrotherFee",  coBrotherFee,
                    "coBrotherOptIn", coBrotherOptIn,
                    "currency",      "INR",
                    "softwareId",    softwareId,
                    "keyId",         razorpayKeyId
            ));
        } catch (RazorpayException e) {
            return ResponseEntity.internalServerError()
                    .body("Payment order creation failed: " + e.getMessage());
        }
    }

    public ResponseEntity<?> createCoBrotherHelpOrder(Long purchaseId, AppUser buyer) {
        try {
            SoftwarePurchase purchase = purchaseRepository.findById(purchaseId).orElse(null);
            if (purchase == null) return ResponseEntity.notFound().build();
            if (!purchase.getBuyer().getId().equals(buyer.getId()))
                return ResponseEntity.status(403).body("Not your purchase");
            if (purchase.getPaymentStatus() != SoftwarePaymentStatus.COMPLETED)
                return ResponseEntity.badRequest().body("Purchase not completed");
            if (purchase.isCoBrotherHelpPaid())
                return ResponseEntity.badRequest().body("Already paid");

            RazorpayClient client = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
            JSONObject req = new JSONObject();
            req.put("amount", 100000); // ₹1000
            req.put("currency", "INR");
            req.put("receipt", "cbhelp_" + purchaseId);
            Order order = client.orders.create(req);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("orderId",  order.get("id").toString());
            response.put("amount",   1000.0);
            response.put("currency", "INR");
            response.put("keyId",    razorpayKeyId);
            return ResponseEntity.ok(response);
        } catch (RazorpayException e) {
            return ResponseEntity.internalServerError().body("Failed: " + e.getMessage());
        }
    }

    public ResponseEntity<?> verifyCoBrotherHelp(Long purchaseId, AppUser buyer,
                                                 String paymentId, String orderId, String signature) {
        try {
            SoftwarePurchase purchase = purchaseRepository.findById(purchaseId).orElse(null);
            if (purchase == null) return ResponseEntity.notFound().build();
            if (!purchase.getBuyer().getId().equals(buyer.getId()))
                return ResponseEntity.status(403).body("Not your purchase");

            String expected = hmacSHA256(orderId + "|" + paymentId, razorpayKeySecret);
            if (!expected.equals(signature))
                return ResponseEntity.badRequest().body(Map.of("success", false));

            purchase.setCoBrotherOptIn(true);
            purchase.setCoBrotherHelpPaid(true);
            purchaseRepository.save(purchase);

            // Auto-create CoBrotherRequest — admin can now forward it
            createCoCrationRequest(purchase, purchase.getSoftware());

            Software sw = purchase.getSoftware();
            if (sw != null && sw.getListedBy() != null) {
                notificationService.notify(
                        sw.getListedBy(),
                        com.cobrother.web.Entity.notification.NotificationType.COVENTURE_APPLICATION_RECEIVED,
                        "CoBrother Addon Activated",
                        purchase.getBuyerFullName() + " has activated the CoBrother helper addon for \"" +
                                sw.getName() + "\". The admin can now forward a CoBrother request.",
                        "/dashboard"
                );
            }

            return ResponseEntity.ok(Map.of("success", true,
                    "message", "CoBrother help activated!"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    private void createCoCrationRequest(SoftwarePurchase purchase, Software sw) {
        // If a PENDING request already exists for this purchase, update its entityDetails
        // to reflect the latest coBrotherHelpPaid flag (e.g. buyer paid addon later).
        Optional<CoBrotherRequest> existingOpt = coBrotherRequestRepository
                .findFirstByEntityIdAndRequestTypeAndStatus(
                        purchase.getId(),
                        RequestType.COCREATION,
                        CoBrotherRequestStatus.PENDING);

        if (existingOpt.isPresent()) {
            CoBrotherRequest existing = existingOpt.get();
            // Rebuild entityDetails with up-to-date coBrotherHelpPaid
            existing.setEntityDetails(buildEntityDetails(purchase, sw));
            coBrotherRequestRepository.save(existing);
            return;
        }

        // Guard: if a non-PENDING active request exists (FORWARDED / ACCEPTED etc.) don't create another
        boolean activeExists = coBrotherRequestRepository
                .existsByEntityIdAndRequestTypeAndStatusNotIn(
                        purchase.getId(),
                        RequestType.COCREATION,
                        List.of(CoBrotherRequestStatus.CANCELLED, CoBrotherRequestStatus.REJECTED,
                                CoBrotherRequestStatus.PENDING));
        if (activeExists) return;

        AppUser lister = sw.getListedBy();

        String buyerName  = purchase.getBuyerFullName() != null ? purchase.getBuyerFullName()
                : (purchase.getBuyer() != null
                ? purchase.getBuyer().getFirstname() + " " + purchase.getBuyer().getLastname() : "");
        String buyerEmail = purchase.getBuyerEmail() != null ? purchase.getBuyerEmail()
                : (purchase.getBuyer() != null ? purchase.getBuyer().getEmail() : "");
        String buyerPhone = purchase.getBuyerPhone() != null ? purchase.getBuyerPhone()
                : (purchase.getBuyer() != null ? purchase.getBuyer().getPhoneNumber() : "");

        CoBrotherRequest req = new CoBrotherRequest();
        req.setRequestType(RequestType.COCREATION);
        req.setEntityId(purchase.getId());
        req.setStatus(CoBrotherRequestStatus.PENDING);   // no CoBrother assigned yet
        // assignedCoBrother left null — admin picks via Forward modal

        req.setLister(lister);
        req.setListerName(lister.getFirstname() + " " + lister.getLastname());
        req.setListerEmail(lister.getEmail());
        req.setListerPhone(lister.getPhoneNumber());

        req.setApplicantName(buyerName);
        req.setApplicantEmail(buyerEmail);
        req.setApplicantPhone(buyerPhone);

        req.setEntityTitle("Co-Creation Purchase Request — " + sw.getName());
        req.setPaidAt(purchase.getSoldAt() != null ? purchase.getSoldAt() : LocalDateTime.now());

        req.setEntityDetails(buildEntityDetails(purchase, sw));

        coBrotherRequestRepository.save(req);
    }

    /** Builds the entityDetails JSON string for a COCREATION CoBrotherRequest. */
    private String buildEntityDetails(SoftwarePurchase purchase, Software sw) {
        String buyerName  = purchase.getBuyerFullName() != null ? purchase.getBuyerFullName()
                : (purchase.getBuyer() != null
                ? purchase.getBuyer().getFirstname() + " " + purchase.getBuyer().getLastname() : "");
        String buyerEmail = purchase.getBuyerEmail() != null ? purchase.getBuyerEmail()
                : (purchase.getBuyer() != null ? purchase.getBuyer().getEmail() : "");
        String buyerPhone = purchase.getBuyerPhone() != null ? purchase.getBuyerPhone()
                : (purchase.getBuyer() != null ? purchase.getBuyer().getPhoneNumber() : "");

        return "{" +
                "\"type\":\"CoCrationPurchase\"," +
                "\"purchaseId\":" + purchase.getId() + "," +
                "\"softwareId\":" + sw.getId() + "," +
                "\"softwareName\":\"" + sw.getName() + "\"," +
                "\"price\":" + sw.getPrice() + "," +
                "\"category\":\"" + (sw.getCategory() != null ? sw.getCategory().name() : "") + "\"," +
                "\"techStack\":\"" + (sw.getTechStack() != null
                ? sw.getTechStack().replace("\"", "'") : "") + "\"," +
                "\"buyerName\":\"" + buyerName.replace("\"", "'") + "\"," +
                "\"buyerEmail\":\"" + buyerEmail + "\"," +
                "\"buyerPhone\":\"" + buyerPhone + "\"," +
                "\"coBrotherHelpPaid\":" + purchase.isCoBrotherHelpPaid() +
                "}";
    }
}