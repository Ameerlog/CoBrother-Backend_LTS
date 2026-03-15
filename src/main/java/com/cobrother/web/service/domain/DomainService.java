package com.cobrother.web.service.domain;

import com.cobrother.web.Entity.cobranding.*;
import com.cobrother.web.Entity.user.AppUser;
import com.cobrother.web.Repository.DomainRepository;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class DomainService {

    @Autowired
    private DomainRepository domainRepository;

    @Autowired
    private MailService mailService;

    @Value("${razorpay.key-id}")
    private String razorpayKeyId;

    @Value("${razorpay.key-secret}")
    private String razorpayKeySecret;

    @Autowired
    private NotificationService notificationService;

    public ResponseEntity<Domain> getDomain(long id) {
        try {
            return ResponseEntity.ok(domainRepository.getDomainById(id));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    public ResponseEntity<List<Domain>> getAllDomains() {
        return ResponseEntity.ok(domainRepository.findByStatusTrue());
    }

    public ResponseEntity<List<Domain>> getMyListedDomains(AppUser user) {
        return ResponseEntity.ok(domainRepository.findByListedBy(user));
    }

    public ResponseEntity<List<Domain>> getMyPurchasedDomains(AppUser user) {
        return ResponseEntity.ok(domainRepository.findByPurchasedBy(user));
    }

    public ResponseEntity<?> addDomain(Domain domain, AppUser lister) {
        try {
            String name = domain.getDomainName();
            String ext  = domain.getDomainExtension();

            // ── 1. Check for active listing of same domain by anyone ─────────────
            Optional<Domain> activeListing = domainRepository
                    .findByDomainNameAndDomainExtensionAndStatusTrue(name, ext);

            if (activeListing.isPresent()) {
                Domain existing = activeListing.get();
                // Same user trying to re-list their own active domain
                if (existing.getListedBy().getId().equals(lister.getId())) {
                    return ResponseEntity.badRequest().body(Map.of(
                            "error", "You already have an active listing for " + name + ext +
                                    ". If you want to re-list it, please take the existing listing down first."
                    ));
                }
                // Different user trying to list an actively listed domain
                return ResponseEntity.badRequest().body(Map.of(
                        "error", name + ext + " is already listed by another seller."
                ));
            }

            // ── 2. Check cooldown for sold domains (2 days) ──────────────────────
            List<Domain> soldListings = domainRepository
                    .findByDomainNameAndDomainExtension(name, ext)
                    .stream()
                    .filter(d -> d.getDomainStatus() == DomainStatus.SOLD && d.getSoldAt() != null)
                    .collect(java.util.stream.Collectors.toList());

            if (!soldListings.isEmpty()) {
                Domain lastSold = soldListings.stream()
                        .max(java.util.Comparator.comparing(Domain::getSoldAt))
                        .orElse(null);
                if (lastSold != null) {
                    LocalDateTime cooldownEnds = lastSold.getSoldAt().plusDays(2);
                    if (LocalDateTime.now().isBefore(cooldownEnds)) {
                        long hoursLeft = java.time.Duration.between(
                                LocalDateTime.now(), cooldownEnds).toHours();
                        return ResponseEntity.badRequest().body(Map.of(
                                "error", name + ext + " was recently sold. It can be re-listed in " +
                                        hoursLeft + " hours."
                        ));
                    }
                }
            }

            // ── 3. Check if domain is already verified by someone else ───────────
            // If the same domain was previously verified by a different user,
            // the new lister must re-verify (don't carry over old verification)
            domain.setListedBy(lister);
            domain.setStatus(true);
            domain.setDomainStatus(DomainStatus.AVAILABLE);
            domain.setVerified(false);
            domain.setVerifiedAt(null);
            domain.setVerificationMethod(null);
            domain.setWhoisEmail(null);
            domain.setVerificationToken(
                    "cobrother-verify=" + UUID.randomUUID().toString().replace("-", "")
            );
            return ResponseEntity.ok(domainRepository.save(domain));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to list domain."));
        }
    }

    public ResponseEntity<Domain> updateDomain(long id, Domain domain, AppUser user) {

        // In updateDomain() and deleteDomain() — add before mutating
        if (!domain.getListedBy().getId().equals(user.getId())) {
            return ResponseEntity.status(403).build();
        }

        try {
            Domain existing = domainRepository.getDomainById(id);
            if (existing != null) {
                existing.setDomainName(domain.getDomainName());
                existing.setDomainExtension(domain.getDomainExtension());
                existing.setDomainCategory(domain.getDomainCategory());
                existing.setAskingPrice(domain.getAskingPrice());
                existing.setPricingDemand(domain.getPricingDemand());
                existing.setContactInfo(domain.getContactInfo());
                return ResponseEntity.ok(domainRepository.save(existing));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.badRequest().build();
    }

    public ResponseEntity<Domain> deleteDomain(long id, AppUser user) {

        try {
            Domain domain = domainRepository.getDomainById(id);

            // In updateDomain() and deleteDomain() — add before mutating
            if (!domain.getListedBy().getId().equals(user.getId())) {
                return ResponseEntity.status(403).build();
            }
            domain.setStatus(false);
            return ResponseEntity.ok(domainRepository.save(domain));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    // ── Razorpay: Create order ────────────────────────────────────────────────
    public ResponseEntity<?> createPurchaseOrder(long domainId, AppUser buyer) {
        try {
            Domain domain = domainRepository.getDomainById(domainId);
            if (domain == null)
                return ResponseEntity.notFound().build();
            if (domain.getDomainStatus() != DomainStatus.AVAILABLE)
                return ResponseEntity.badRequest().body("Domain is not available for purchase");
            if (domain.getListedBy().getId().equals(buyer.getId()))
                return ResponseEntity.badRequest().body("You cannot buy your own domain");

            RazorpayClient client = new RazorpayClient(razorpayKeyId, razorpayKeySecret);

            JSONObject orderRequest = new JSONObject();
            // Razorpay expects amount in paise (1 INR = 100 paise)
            orderRequest.put("amount", (int)(domain.getAskingPrice() * 100));
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "domain_" + domainId + "_" + buyer.getId());
            orderRequest.put("notes", new JSONObject()
                    .put("domainId", domainId)
                    .put("buyerId", buyer.getId()));

            Order order = client.orders.create(orderRequest);

            // Mark domain as PENDING and save order ID
            domain.setDomainStatus(DomainStatus.PENDING);
            domain.setRazorpayOrderId(order.get("id"));
            domain.setPaymentStatus(PaymentStatus.CREATED);
            domain.setPurchasedBy(buyer);
            domainRepository.save(domain);

            return ResponseEntity.ok(Map.of(
                    "orderId", order.get("id").toString(),
                    "amount", domain.getAskingPrice(),
                    "currency", "INR",
                    "domainId", domainId,
                    "keyId", razorpayKeyId
            ));
        } catch (RazorpayException e) {
            return ResponseEntity.internalServerError().body("Payment order creation failed: " + e.getMessage());
        }
    }

    // ── Razorpay: Verify payment ──────────────────────────────────────────────
    public ResponseEntity<?> verifyPayment(long domainId, String razorpayPaymentId,
                                           String razorpayOrderId, String razorpaySignature,
                                           AppUser buyer) {
        try {
            Domain domain = domainRepository.getDomainById(domainId);
            if (domain == null) return ResponseEntity.notFound().build();

            // Verify signature
            String payload = razorpayOrderId + "|" + razorpayPaymentId;
            String expectedSignature = hmacSHA256(payload, razorpayKeySecret);

            if (!expectedSignature.equals(razorpaySignature)) {
                domain.setDomainStatus(DomainStatus.AVAILABLE);
                domain.setPaymentStatus(PaymentStatus.FAILED);
                domain.setPurchasedBy(null);
                domainRepository.save(domain);
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Payment verification failed"));
            }

            // Payment verified — mark as SOLD
            domain.setDomainStatus(DomainStatus.SOLD);
            domain.setSoldAt(LocalDateTime.now());
            domain.setPaymentStatus(PaymentStatus.COMPLETED);
            domain.setRazorpayPaymentId(razorpayPaymentId);
            domain.setPurchasedBy(buyer);
            domainRepository.save(domain);

            notificationService.notifyDomainSold(
                    domain.getListedBy(),
                    domain.getDomainName() + domain.getDomainExtension(),
                    buyer.getFirstname() + " " + buyer.getLastname()
            );

            // Send emails
            mailService.sendDomainPurchaseBuyerEmail(
                    buyer.getEmail(),
                    buyer.getFirstname(),
                    domain.getDomainName() + domain.getDomainExtension(),
                    domain.getAskingPrice(),
                    razorpayPaymentId
            );
            mailService.sendDomainPurchaseSellerEmail(
                    domain.getListedBy().getEmail(),
                    domain.getListedBy().getFirstname(),
                    domain.getDomainName() + domain.getDomainExtension(),
                    buyer.getFirstname() + " " + buyer.getLastname(),
                    domain.getAskingPrice()
            );

            return ResponseEntity.ok(Map.of("success", true, "message", "Payment successful"));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Verification error: " + e.getMessage());
        }
    }

    // ── Razorpay: Handle failure ──────────────────────────────────────────────
    public ResponseEntity<?> handlePaymentFailure(long domainId) {
        try {
            Domain domain = domainRepository.getDomainById(domainId);
            if (domain != null) {
                domain.setDomainStatus(DomainStatus.AVAILABLE);
                domain.setPaymentStatus(PaymentStatus.FAILED);
                domain.setPurchasedBy(null);
                domain.setRazorpayOrderId(null);
                domainRepository.save(domain);
            }
            return ResponseEntity.ok(Map.of("success", false, "message", "Payment failed"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // ── HMAC-SHA256 signature verification ───────────────────────────────────
    private String hmacSHA256(String data, String key) throws Exception {
        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
        javax.crypto.spec.SecretKeySpec secretKeySpec =
                new javax.crypto.spec.SecretKeySpec(key.getBytes("UTF-8"), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] hash = mac.doFinal(data.getBytes("UTF-8"));
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) hexString.append(String.format("%02x", b));
        return hexString.toString();
    }


}