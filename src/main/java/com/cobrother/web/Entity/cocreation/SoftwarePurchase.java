package com.cobrother.web.Entity.cocreation;

import com.cobrother.web.Entity.user.AppUser;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "software_purchases")
public class SoftwarePurchase {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "software_id", nullable = false)
    @JsonIgnoreProperties({"purchases","softwareViews","listedBy","agreement","hibernateLazyInitializer"})
    private Software software;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    @JsonIgnoreProperties({"password","otp","otpExpiry","emailOtp","emailOtpExpiry",
            "verificationToken","verificationTokenExpiry","refreshToken","hibernateLazyInitializer",
            "listedSoftware","purchasedSoftware","listedDomains","purchasedDomains",
            "listedVentures","coVenturedVentures","communityProfile"})
    private AppUser buyer;

    private String buyerFullName;
    private String buyerEmail;
    private String buyerPhone;

    private String razorpayOrderId;
    private String razorpayPaymentId;

    @Enumerated(EnumType.STRING)
    private SoftwarePaymentStatus paymentStatus;


    private boolean coBrotherOptIn   = false;
    private boolean coBrotherHelpPaid = false;

    private LocalDateTime soldAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist protected void onCreate() { this.createdAt = LocalDateTime.now(); }

    public SoftwarePurchase() {}

    // Getters & Setters
    public Long getId() { return id; }
    public Software getSoftware() { return software; }
    public void setSoftware(Software s) { this.software = s; }
    public AppUser getBuyer() { return buyer; }
    public void setBuyer(AppUser b) { this.buyer = b; }
    public String getBuyerFullName() { return buyerFullName; }
    public void setBuyerFullName(String v) { this.buyerFullName = v; }
    public String getBuyerEmail() { return buyerEmail; }
    public void setBuyerEmail(String v) { this.buyerEmail = v; }
    public String getBuyerPhone() { return buyerPhone; }
    public void setBuyerPhone(String v) { this.buyerPhone = v; }
    public String getRazorpayOrderId() { return razorpayOrderId; }
    public void setRazorpayOrderId(String v) { this.razorpayOrderId = v; }
    public String getRazorpayPaymentId() { return razorpayPaymentId; }
    public void setRazorpayPaymentId(String v) { this.razorpayPaymentId = v; }
    public SoftwarePaymentStatus getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(SoftwarePaymentStatus v) { this.paymentStatus = v; }
    public boolean isCoBrotherOptIn() { return coBrotherOptIn; }
    public void setCoBrotherOptIn(boolean v) { this.coBrotherOptIn = v; }
    public boolean isCoBrotherHelpPaid() { return coBrotherHelpPaid; }
    public void setCoBrotherHelpPaid(boolean v) { this.coBrotherHelpPaid = v; }
    public LocalDateTime getSoldAt() { return soldAt; }
    public void setSoldAt(LocalDateTime v) { this.soldAt = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}