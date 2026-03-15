package com.cobrother.web.service.cocreation;

import com.cobrother.web.Entity.cocreation.*;
import com.cobrother.web.Entity.user.AppUser;
import com.cobrother.web.Repository.CommunityRepository;
import com.cobrother.web.Repository.SoftwareRepository;
import com.cobrother.web.Repository.SoftwareViewRepository;
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

    @Value("${razorpay.key-id}")
    private String razorpayKeyId;

    @Value("${razorpay.key-secret}")
    private String razorpayKeySecret;

    @Autowired
    private NotificationService notificationService;

    // ── CRUD ──────────────────────────────────────────────────────────────────

    public ResponseEntity<List<Software>> getAll() {
        return ResponseEntity.ok(softwareRepository.findByStatusTrue());
    }

    public ResponseEntity<Software> getById(Long id, AppUser viewer) {
        Software software = softwareRepository.findSoftwareById(id);
        if (software == null) return ResponseEntity.notFound().build();

        // Track view
        trackView(software, viewer);

        // Only expose github link if buyer has confirmed purchase
        if (viewer == null
                || software.getPurchasedBy() == null
                || !software.getPurchasedBy().getId().equals(viewer.getId())
                || software.getPaymentStatus() != SoftwarePaymentStatus.COMPLETED) {
            software.setGithubLink(null);
        }

        return ResponseEntity.ok(software);
    }

    public ResponseEntity<List<Software>> getMyListings(AppUser user) {
        // Owner sees everything including github link
        return ResponseEntity.ok(softwareRepository.findByListedBy(user));
    }

    public ResponseEntity<List<Software>> getMyPurchases(AppUser user) {
        List<Software> purchases = softwareRepository.findByPurchasedBy(user);
        // Expose github only where confirmed
        purchases.forEach(s -> {
            if (s.getPaymentStatus() != SoftwarePaymentStatus.COMPLETED) {
                s.setGithubLink(null);
            }
        });
        return ResponseEntity.ok(purchases);
    }

    public ResponseEntity<Software> create(Software software) {
        try {
            software.setStatus(true);
            software.setSoftwareStatus(SoftwareStatus.AVAILABLE);
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
                                         String buyerFullName, String buyerEmail, String buyerPhone) {
        try {
            Software software = softwareRepository.findSoftwareById(id);
            if (software == null) return ResponseEntity.notFound().build();
            if (software.getSoftwareStatus() != SoftwareStatus.AVAILABLE)
                return ResponseEntity.badRequest().body("Software is not available");
            if (software.getListedBy().getId().equals(buyer.getId()))
                return ResponseEntity.badRequest().body("You cannot buy your own listing");

            RazorpayClient client = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", (int)(software.getPrice() * 100));
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "software_" + id + "_" + buyer.getId());

            Order order = client.orders.create(orderRequest);

            // ✅ Only store order metadata — do NOT set purchasedBy or completionStatus yet
            software.setRazorpayOrderId(order.get("id").toString());
            software.setPaymentStatus(SoftwarePaymentStatus.CREATED);
            // Temporarily store buyer details for the verify step
            software.setBuyerFullName(buyerFullName);
            software.setBuyerEmail(buyerEmail);
            software.setBuyerPhone(buyerPhone);
            softwareRepository.save(software);

            return ResponseEntity.ok(Map.of(
                    "orderId", order.get("id").toString(),
                    "amount", software.getPrice(),
                    "currency", "INR",
                    "softwareId", id,
                    "keyId", razorpayKeyId
            ));
        } catch (RazorpayException e) {
            return ResponseEntity.internalServerError().body("Order creation failed: " + e.getMessage());
        }
    }

    public ResponseEntity<?> verifyPayment(Long id, String paymentId,
                                           String orderId, String signature, AppUser buyer) {
        try {
            Software software = softwareRepository.findSoftwareById(id);
            if (software == null) return ResponseEntity.notFound().build();

            String payload = orderId + "|" + paymentId;
            String expected = hmacSHA256(payload, razorpayKeySecret);

            if (!expected.equals(signature)) {
                software.setPaymentStatus(SoftwarePaymentStatus.FAILED);
                // ✅ Don't set purchasedBy on failure
                softwareRepository.save(software);
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Verification failed"));
            }

            // ✅ Payment confirmed — NOW set purchasedBy and completionStatus
            software.setSoftwareStatus(SoftwareStatus.SOLD);
            software.setPaymentStatus(SoftwarePaymentStatus.COMPLETED);
            software.setRazorpayPaymentId(paymentId);
            software.setSoldAt(LocalDateTime.now());
            software.setPurchasedBy(buyer);                                      // ✅ set here
            software.setCompletionStatus(PurchaseCompletionStatus.PENDING);      // ✅ set here
            softwareRepository.save(software);

            notificationService.notifySoftwarePurchased(
                    software.getListedBy(),
                    software.getName(),
                    buyer.getFirstname() + " " + buyer.getLastname()
            );


            mailService.sendSoftwarePurchaseBuyerEmail(
                    buyer.getEmail(), buyer.getFirstname(),
                    software.getName(), software.getPrice(), paymentId, software.getGithubLink());
            mailService.sendSoftwarePurchaseSellerEmail(
                    software.getListedBy().getEmail(), software.getListedBy().getFirstname(),
                    software.getName(), software.getBuyerFullName(), software.getPrice());

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", true);
            response.put("message", "Payment successful");
            response.put("githubLink", software.getGithubLink() != null ? software.getGithubLink() : "");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    public ResponseEntity<?> handleFailure(Long id) {
        try {
            Software software = softwareRepository.findSoftwareById(id);
            if (software != null) {
                software.setPaymentStatus(SoftwarePaymentStatus.FAILED);
                software.setPurchasedBy(null);        // ✅ ensure null on failure
                software.setRazorpayOrderId(null);
                software.setCompletionStatus(null);   // ✅ ensure null on failure
                software.setBuyerFullName(null);
                software.setBuyerEmail(null);
                software.setBuyerPhone(null);
                softwareRepository.save(software);
            }
            return ResponseEntity.ok(Map.of("success", false));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // ── Buyer confirms everything is fine ─────────────────────────────────────
    public ResponseEntity<?> confirmPurchase(Long id, AppUser buyer) {
        try {
            Software software = softwareRepository.findSoftwareById(id);
            if (software == null) return ResponseEntity.notFound().build();
            if (software.getPurchasedBy() == null
                    || !software.getPurchasedBy().getId().equals(buyer.getId()))
                return ResponseEntity.status(403).body("Not your purchase");
            if (software.getPaymentStatus() != SoftwarePaymentStatus.COMPLETED)
                return ResponseEntity.badRequest().body("Payment not completed");

            software.setCompletionStatus(PurchaseCompletionStatus.CONFIRMED);
            softwareRepository.save(software);

            notificationService.notifySoftwareMarkedComplete(
                    software.getListedBy(),
                    software.getName(),
                    buyer.getFirstname() + " " + buyer.getLastname()
            );

            return ResponseEntity.ok(Map.of("success", true, "githubLink", software.getGithubLink()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
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
        List<Software> allListings = softwareRepository.findByListedBy(user);
        double totalRevenue = allListings.stream()
                .filter(s -> s.getPaymentStatus() == SoftwarePaymentStatus.COMPLETED)
                .mapToDouble(Software::getPrice).sum();

        long totalSales = allListings.stream()
                .filter(s -> s.getPaymentStatus() == SoftwarePaymentStatus.COMPLETED).count();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("softwareId", softwareId);
        result.put("softwareName", software.getName());
        result.put("totalViews", views.size());
        result.put("totalSales", totalSales);
        result.put("totalRevenue", totalRevenue);
        result.put("viewsByDay", viewsByDay);
        result.put("byIndustry", byIndustry);
        result.put("byRole", byRole);
        result.put("completionStatus", software.getCompletionStatus());
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
}