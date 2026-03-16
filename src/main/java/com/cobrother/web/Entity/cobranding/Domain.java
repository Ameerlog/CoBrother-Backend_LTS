package com.cobrother.web.Entity.cobranding;

import com.cobrother.web.Entity.coventure.Agreement;
import com.cobrother.web.Entity.coventure.ContactInfo;
import com.cobrother.web.Entity.user.AppUser;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class Domain {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String domainName;       // e.g. "mybrand"
    private String domainExtension;  // e.g. ".com"

    @Enumerated(EnumType.STRING)
    private DomainCategory domainCategory;

    private double askingPrice;

    @Enumerated(EnumType.STRING)
    private PricingDemand pricingDemand; // NEGOTIABLE, FIXED

    @Enumerated(EnumType.STRING)
    private DomainStatus domainStatus = DomainStatus.AVAILABLE; // AVAILABLE, SOLD, PENDING

    @Embedded
    private ContactInfo contactInfo;

    @Embedded
    private Agreement agreement;

    private String logo;
    private boolean status;
    private long views;

    // Payment fields
    private String razorpayOrderId;
    private String razorpayPaymentId;

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus; // CREATED, COMPLETED, FAILED

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listed_by_user_id")
    @JsonIgnoreProperties({"listedDomains","purchasedDomains","listedVentures",
            "purchasedVentures","coVenturedVentures","communityProfile",
            "password","otp","otpExpiry","emailOtp","emailOtpExpiry",
            "verificationToken","verificationTokenExpiry","refreshToken","hibernateLazyInitializer"})
    private AppUser listedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchased_by_user_id")
    @JsonIgnoreProperties({"listedDomains","purchasedDomains","listedVentures",
            "purchasedVentures","coVenturedVentures","communityProfile",
            "password","otp","otpExpiry","emailOtp","emailOtpExpiry",
            "verificationToken","verificationTokenExpiry","refreshToken","hibernateLazyInitializer"})
    private AppUser purchasedBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private LocalDateTime soldAt;

    private boolean verified = false;

    // Hidden from API response — only used internally for checking
    @JsonIgnore
    private String verificationToken;

    @Enumerated(EnumType.STRING)
    private VerificationMethod verificationMethod;

    private LocalDateTime verifiedAt;

    // For WHOIS_EMAIL — store the masked email so frontend can show "sent to r***@domain.com"
    private String whoisEmail;


    private boolean takenDown = false;

    @Column(columnDefinition = "TEXT")
    private String takeDownReason;

    // Lifecycle — add before the no-arg constructor
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }


    public Domain() {}

    // ── Getters & Setters ────────────────────────────────────────────────────
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDomainName() { return domainName; }
    public void setDomainName(String domainName) { this.domainName = domainName; }
    public String getDomainExtension() { return domainExtension; }
    public void setDomainExtension(String domainExtension) { this.domainExtension = domainExtension; }
    public DomainCategory getDomainCategory() { return domainCategory; }
    public void setDomainCategory(DomainCategory domainCategory) { this.domainCategory = domainCategory; }
    public double getAskingPrice() { return askingPrice; }
    public void setAskingPrice(double askingPrice) { this.askingPrice = askingPrice; }
    public PricingDemand getPricingDemand() { return pricingDemand; }
    public void setPricingDemand(PricingDemand pricingDemand) { this.pricingDemand = pricingDemand; }
    public DomainStatus getDomainStatus() { return domainStatus; }
    public void setDomainStatus(DomainStatus domainStatus) { this.domainStatus = domainStatus; }
    public ContactInfo getContactInfo() { return contactInfo; }
    public void setContactInfo(ContactInfo contactInfo) { this.contactInfo = contactInfo; }
    public Agreement getAgreement() { return agreement; }
    public void setAgreement(Agreement agreement) { this.agreement = agreement; }
    public String getLogo() { return logo; }
    public void setLogo(String logo) { this.logo = logo; }
    public boolean isStatus() { return status; }
    public void setStatus(boolean status) { this.status = status; }
    public long getViews() { return views; }
    public void setViews(long views) { this.views = views; }
    public String getRazorpayOrderId() { return razorpayOrderId; }
    public void setRazorpayOrderId(String razorpayOrderId) { this.razorpayOrderId = razorpayOrderId; }
    public String getRazorpayPaymentId() { return razorpayPaymentId; }
    public void setRazorpayPaymentId(String razorpayPaymentId) { this.razorpayPaymentId = razorpayPaymentId; }
    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(PaymentStatus paymentStatus) { this.paymentStatus = paymentStatus; }
    public AppUser getListedBy() { return listedBy; }
    public void setListedBy(AppUser listedBy) { this.listedBy = listedBy; }
    public AppUser getPurchasedBy() { return purchasedBy; }
    public void setPurchasedBy(AppUser purchasedBy) { this.purchasedBy = purchasedBy; }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getSoldAt() {
        return soldAt;
    }

    public void setSoldAt(LocalDateTime soldAt) {
        this.soldAt = soldAt;
    }

    public VerificationMethod getVerificationMethod() {
        return verificationMethod;
    }

    public void setVerificationMethod(VerificationMethod verificationMethod) {
        this.verificationMethod = verificationMethod;
    }

    public String getVerificationToken() {
        return verificationToken;
    }

    public void setVerificationToken(String verificationToken) {
        this.verificationToken = verificationToken;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public LocalDateTime getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(LocalDateTime verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public String getWhoisEmail() {
        return whoisEmail;
    }

    public void setWhoisEmail(String whoisEmail) {
        this.whoisEmail = whoisEmail;
    }

    public String getTakeDownReason() {
        return takeDownReason;
    }

    public void setTakeDownReason(String takeDownReason) {
        this.takeDownReason = takeDownReason;
    }

    public boolean isTakenDown() {
        return takenDown;
    }

    public void setTakenDown(boolean takenDown) {
        this.takenDown = takenDown;
    }
}