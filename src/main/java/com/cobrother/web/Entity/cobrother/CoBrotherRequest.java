package com.cobrother.web.Entity.cobrother;

import com.cobrother.web.Entity.user.AppUser;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity// Replace @Table annotation
@Table(
        name = "cobrother_requests",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_cobrother_request",
                columnNames = {"entity_id", "request_type", "assigned_cobrother_id"}
        )
)
public class CoBrotherRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestType requestType; // COVENTURE, DOMAIN, COCREATION

    // The original entity ID (coventure id, domain id, or software id)
    @Column(nullable = false)
    private Long entityId;

    // ── Snapshots stored at request creation time ─────────────────────────────
    // Lister details
    private String listerName;
    private String listerEmail;
    private String listerPhone;

    // Applicant / buyer details
    private String applicantName;
    private String applicantEmail;
    private String applicantPhone;

    // Entity details snapshot
    private String entityTitle;   // venture brand name / domain name / software name

    @Column(columnDefinition = "TEXT")
    private String entityDetails; // JSON string of key details

    // ── Relationships ─────────────────────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_cobrother_id")
    @JsonIgnoreProperties({"listedDomains","purchasedDomains","listedVentures",
            "purchasedVentures","coVenturedVentures","communityProfile",
            "password","otp","otpExpiry","emailOtp","emailOtpExpiry",
            "verificationToken","verificationTokenExpiry","refreshToken",
            "hibernateLazyInitializer","listedSoftware","purchasedSoftware"})
    private AppUser assignedCoBrother;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_admin_id")
    @JsonIgnoreProperties({"listedDomains","purchasedDomains","listedVentures",
            "purchasedVentures","coVenturedVentures","communityProfile",
            "password","otp","otpExpiry","emailOtp","emailOtpExpiry",
            "verificationToken","verificationTokenExpiry","refreshToken",
            "hibernateLazyInitializer","listedSoftware","purchasedSoftware"})
    private AppUser createdByAdmin;

    // The lister (who must pay ₹1000)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lister_user_id")
    @JsonIgnoreProperties({"listedDomains","purchasedDomains","listedVentures",
            "purchasedVentures","coVenturedVentures","communityProfile",
            "password","otp","otpExpiry","emailOtp","emailOtpExpiry",
            "verificationToken","verificationTokenExpiry","refreshToken",
            "hibernateLazyInitializer","listedSoftware","purchasedSoftware"})
    private AppUser lister;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CoBrotherRequestStatus status = CoBrotherRequestStatus.PAYMENT_PENDING;

    // ── Payment fields ────────────────────────────────────────────────────────
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private LocalDateTime paidAt;

    // CoBrother response
    private String coBrotherNote; // optional note when accepting/rejecting
    private LocalDateTime respondedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() { this.updatedAt = LocalDateTime.now(); }

    // ── Getters & Setters ─────────────────────────────────────────────────────
    public Long getId() { return id; }
    public RequestType getRequestType() { return requestType; }
    public void setRequestType(RequestType requestType) { this.requestType = requestType; }
    public Long getEntityId() { return entityId; }
    public void setEntityId(Long entityId) { this.entityId = entityId; }
    public String getListerName() { return listerName; }
    public void setListerName(String listerName) { this.listerName = listerName; }
    public String getListerEmail() { return listerEmail; }
    public void setListerEmail(String listerEmail) { this.listerEmail = listerEmail; }
    public String getListerPhone() { return listerPhone; }
    public void setListerPhone(String listerPhone) { this.listerPhone = listerPhone; }
    public String getApplicantName() { return applicantName; }
    public void setApplicantName(String applicantName) { this.applicantName = applicantName; }
    public String getApplicantEmail() { return applicantEmail; }
    public void setApplicantEmail(String applicantEmail) { this.applicantEmail = applicantEmail; }
    public String getApplicantPhone() { return applicantPhone; }
    public void setApplicantPhone(String applicantPhone) { this.applicantPhone = applicantPhone; }
    public String getEntityTitle() { return entityTitle; }
    public void setEntityTitle(String entityTitle) { this.entityTitle = entityTitle; }
    public String getEntityDetails() { return entityDetails; }
    public void setEntityDetails(String entityDetails) { this.entityDetails = entityDetails; }
    public AppUser getAssignedCoBrother() { return assignedCoBrother; }
    public void setAssignedCoBrother(AppUser assignedCoBrother) { this.assignedCoBrother = assignedCoBrother; }
    public AppUser getCreatedByAdmin() { return createdByAdmin; }
    public void setCreatedByAdmin(AppUser createdByAdmin) { this.createdByAdmin = createdByAdmin; }
    public AppUser getLister() { return lister; }
    public void setLister(AppUser lister) { this.lister = lister; }
    public CoBrotherRequestStatus getStatus() { return status; }
    public void setStatus(CoBrotherRequestStatus status) { this.status = status; }
    public String getRazorpayOrderId() { return razorpayOrderId; }
    public void setRazorpayOrderId(String razorpayOrderId) { this.razorpayOrderId = razorpayOrderId; }
    public String getRazorpayPaymentId() { return razorpayPaymentId; }
    public void setRazorpayPaymentId(String razorpayPaymentId) { this.razorpayPaymentId = razorpayPaymentId; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
    public String getCoBrotherNote() { return coBrotherNote; }
    public void setCoBrotherNote(String coBrotherNote) { this.coBrotherNote = coBrotherNote; }
    public LocalDateTime getRespondedAt() { return respondedAt; }
    public void setRespondedAt(LocalDateTime respondedAt) { this.respondedAt = respondedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
