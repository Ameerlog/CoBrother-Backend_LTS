package com.cobrother.web.service.domain;

import com.cobrother.web.Entity.cobranding.*;
import com.cobrother.web.Entity.user.AppUser;
import com.cobrother.web.Entity.user.UserRole;
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

    @Autowired
    private com.cobrother.web.Repository.DomainEnquiryRepository domainEnquiryRepository;

    public ResponseEntity<Domain> getDomain(long id) {
        try {
            Domain domain = domainRepository.getDomainByIdAndStatusTrue(id);
            if (domain == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(domain);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    public ResponseEntity<List<Domain>> getAllDomains() {
        return ResponseEntity.ok(domainRepository.findByStatusTrue());
    }

    // Add a new endpoint for "my listings including taken down"
    public ResponseEntity<List<Domain>> getMyListedDomainsAll(AppUser user) {
        // Returns all including taken down — for lister's own dashboard view
        return ResponseEntity.ok(domainRepository.findByListedBy(user));
    }

    public ResponseEntity<List<Domain>> getMyPurchasedDomains(AppUser user) {
        return ResponseEntity.ok(domainRepository.findByPurchasedBy(user));
    }

    public ResponseEntity<?> addDomain(Domain domain, AppUser lister) {
        try {
            String name = domain.getDomainName();
            String ext  = domain.getDomainExtension();

            // ── 1. Check for active verified listing of same domain by anyone ─────────────

            Optional<Domain> verifiedListing = domainRepository
                    .findByDomainNameAndDomainExtensionAndStatusTrue(name, ext)
                    .stream()
                    // filter to only verified ones
                    .filter(Domain::isVerified)
                    .findFirst();

            Optional<Domain> ownActiveListing = domainRepository
                    .findByDomainNameAndDomainExtensionAndStatusTrue(name, ext)
                    .stream()
                    .filter(d -> d.getListedBy().getId().equals(lister.getId()))
                    .findFirst();


            if (ownActiveListing.isPresent()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "You already have an active listing for " + name + ext +
                                ". Take it down before re-listing."
                ));
            }

            if (verifiedListing.isPresent()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", name + ext + " is already listed and verified by another seller."
                ));
            }

            // ── 2. Check cooldown for sold domains (2 days) ──────────────────────
            // ── 2. Cooldown + previous seller restriction ─────────────────────────────
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
                    // Previous seller cannot re-list
                    if (lastSold.getListedBy().getId().equals(lister.getId())) {
                        return ResponseEntity.badRequest().body(Map.of(
                                "error", "As the previous seller, you cannot re-list " + name + ext + "."
                        ));
                    }
                    // 2-day cooldown for everyone else
                    LocalDateTime cooldownEnds = lastSold.getSoldAt().plusDays(2);
                    if (LocalDateTime.now().isBefore(cooldownEnds)) {
                        long hoursLeft = java.time.Duration.between(
                                LocalDateTime.now(), cooldownEnds).toHours();
                        return ResponseEntity.badRequest().body(Map.of(
                                "error", name + ext + " was recently sold. Re-listing opens in " +
                                        hoursLeft + " hour" + (hoursLeft != 1 ? "s" : "") + "."
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
            // pricingDemand no longer collected from form — default to NEGOTIABLE
            if (domain.getPricingDemand() == null) {
                domain.setPricingDemand(PricingDemand.NEGOTIABLE);
            }
            domain.setVerificationToken(
                    "cobrother-verify=" + UUID.randomUUID().toString().replace("-", "")
            );

            // In addDomain(), after the existing validation checks, before saving:
            if (domain.getSaleType() == SaleType.AUCTION) {
                // Auction domains start as DRAFT — verified separately
                // Don't set status to true yet — make visible so lister can see it in dashboard
                // status=false keeps it off public listings until auction is ACTIVE
                domain.setStatus(false); // hidden from public until auction activates
                domain.setDomainStatus(DomainStatus.AVAILABLE);
            }
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

    public ResponseEntity<Void> deleteDomain(long id, AppUser user) {

        try {
            Domain domain = domainRepository.getDomainById(id);
            if (domain == null) {
                return ResponseEntity.notFound().build();
            }

            boolean isAdmin = user.getRole() == UserRole.ADMIN;
            boolean isOwner = domain.getListedBy() != null && domain.getListedBy().getId().equals(user.getId());

            if (!isAdmin && !isOwner) {
                return ResponseEntity.status(403).build();
            }

            domainRepository.delete(domain);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
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

    // ── Domain Enquiry ────────────────────────────────────────────────────────
    public ResponseEntity<?> submitEnquiry(long domainId, AppUser enquirer,
                                           String fullName, String email,
                                           String phone, String message) {
        try {
            Domain domain = domainRepository.getDomainById(domainId);
            if (domain == null) return ResponseEntity.notFound().build();
            if (domain.getAskingPrice() < 500000)
                return ResponseEntity.badRequest()
                        .body(java.util.Map.of("error", "Enquiries are only for domains above ₹5,00,000"));

            DomainEnquiry enquiry = new DomainEnquiry();
            enquiry.setDomain(domain);
            enquiry.setEnquirer(enquirer);
            enquiry.setFullName(fullName);
            enquiry.setEmail(email);
            enquiry.setPhone(phone);
            enquiry.setMessage(message);
            enquiry.setStatus(DomainEnquiryStatus.PENDING);
            domainEnquiryRepository.save(enquiry);

            // Notify admin by email (reuse existing mail infra if available)
//            try {
////                mailService.sendDomainEnquiryAdminEmail(
////                        domain.getDomainName() + domain.getDomainExtension(),
////                        fullName, email, phone, message, domain.getAskingPrice());
//            } catch (Exception ignored) {
//                // Email notification is non-critical — don't fail the request
//            }

            return ResponseEntity.ok(java.util.Map.of("success", true,
                    "message", "Enquiry submitted successfully"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(java.util.Map.of("error", "Failed to submit enquiry: " + e.getMessage()));
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