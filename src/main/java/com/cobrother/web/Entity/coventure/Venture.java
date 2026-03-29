package com.cobrother.web.Entity.coventure;

import com.cobrother.web.Entity.user.AppUser;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@JsonIgnoreProperties(ignoreUnknown = true)
public class Venture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Embedded
    private BrandDetails brandDetails;

    @Embedded
    private ContactInfo contactInfo;

    @Embedded
    private Agreement agreement;

    private boolean status;
    private long views;

    @Column(nullable = false, columnDefinition = "bigint default 0")
    private long coVentureApplicationCount = 0;

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

    @Enumerated(EnumType.STRING)
    private VentureStage stage;

    // ── Roles — replaces the lookingFor JSON string ───────────────────────────
    @OneToMany(
            mappedBy = "venture",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @OrderBy("sortOrder ASC")
    @JsonIgnoreProperties("venture")
    private List<VentureRole> roles = new ArrayList<>();

    @Column(columnDefinition = "TEXT")
    private String currentProblem;

    private boolean takenDown = false;

    @Column(columnDefinition = "TEXT")
    private String takeDownReason;

    @OneToMany(mappedBy = "venture", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties("venture")
    private List<CoVenture> coVentureApplications = new ArrayList<>();

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
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ─── Getters & Setters ────────────────────────────────────────────────────
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public BrandDetails getBrandDetails() { return brandDetails; }
    public void setBrandDetails(BrandDetails brandDetails) { this.brandDetails = brandDetails; }

    public ContactInfo getContactInfo() { return contactInfo; }
    public void setContactInfo(ContactInfo contactInfo) { this.contactInfo = contactInfo; }

    public Agreement getAgreement() { return agreement; }
    public void setAgreement(Agreement agreement) { this.agreement = agreement; }

    public boolean isStatus() { return status; }
    public void setStatus(boolean status) { this.status = status; }

    public AppUser getListedBy() { return listedBy; }
    public void setListedBy(AppUser listedBy) { this.listedBy = listedBy; }

    public AppUser getPurchasedBy() { return purchasedBy; }
    public void setPurchasedBy(AppUser purchasedBy) { this.purchasedBy = purchasedBy; }

    public long getViews() { return views; }
    public void setViews(long views) { this.views = views; }

    public long getCoVentureApplicationCount() { return coVentureApplicationCount; }
    public void setCoVentureApplicationCount(long count) { this.coVentureApplicationCount = count; }

    public List<CoVenture> getCoVentureApplications() { return coVentureApplications; }
    public void setCoVentureApplications(List<CoVenture> apps) { this.coVentureApplications = apps; }

    public VentureStage getStage() { return stage; }
    public void setStage(VentureStage stage) { this.stage = stage; }

    public List<VentureRole> getRoles() { return roles; }
    public void setRoles(List<VentureRole> roles) { this.roles = roles; }

    public String getCurrentProblem() { return currentProblem; }
    public void setCurrentProblem(String currentProblem) { this.currentProblem = currentProblem; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getTakeDownReason() { return takeDownReason; }
    public void setTakeDownReason(String takeDownReason) { this.takeDownReason = takeDownReason; }

    public boolean isTakenDown() { return takenDown; }
    public void setTakenDown(boolean takenDown) { this.takenDown = takenDown; }
}