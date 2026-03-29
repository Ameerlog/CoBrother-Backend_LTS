package com.cobrother.web.Entity.cocreation;

import com.cobrother.web.Entity.coventure.Agreement;
import com.cobrother.web.Entity.user.AppUser;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "software")
public class Software {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String videoLink;       // demo video URL

    @Column(columnDefinition = "TEXT")
    private String whatItDoes;

    @Column(columnDefinition = "TEXT")
    private String howItHelps;

    private String githubLink;

    private String liveDemoLink;    // optional

    private String techStack;       // comma-separated: "React, Spring Boot, PostgreSQL"

    @Enumerated(EnumType.STRING)
    private SoftwareCategory category;

    @Enumerated(EnumType.STRING)
    private SoftwarePricingDemand pricingDemand;

    private double price;

    @Enumerated(EnumType.STRING)
    private SoftwareStatus softwareStatus = SoftwareStatus.AVAILABLE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SoftwarePurchaseType purchaseType = SoftwarePurchaseType.ONE_TIME;

    private boolean status = true;  // active/inactive listing
    private long views = 0;

    @Embedded
    private Agreement agreement;


    private boolean takenDown = false;

    @Column(columnDefinition = "TEXT")
    private String takeDownReason;


    private boolean official = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listed_by_user_id")
    @JsonIgnoreProperties({"listedDomains","purchasedDomains","listedVentures",
            "purchasedVentures","coVenturedVentures","communityProfile",
            "password","otp","otpExpiry","emailOtp","emailOtpExpiry",
            "verificationToken","verificationTokenExpiry","refreshToken","hibernateLazyInitializer"})
    private AppUser listedBy;

    @OneToMany(mappedBy = "software", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<SoftwareView> softwareViews = new ArrayList<>();

    @OneToMany(mappedBy = "software", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<SoftwarePurchase> purchases = new ArrayList<>();

    // Add a transient field for purchase count (set by service layer)
    @Transient
    private long purchaseCount;


    @Version
    private Long version;


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

    // ── Getters & Setters ─────────────────────────────────────────────────────
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getVideoLink() { return videoLink; }
    public void setVideoLink(String videoLink) { this.videoLink = videoLink; }
    public String getWhatItDoes() { return whatItDoes; }
    public void setWhatItDoes(String whatItDoes) { this.whatItDoes = whatItDoes; }
    public String getHowItHelps() { return howItHelps; }
    public void setHowItHelps(String howItHelps) { this.howItHelps = howItHelps; }
    public String getGithubLink() { return githubLink; }
    public void setGithubLink(String githubLink) { this.githubLink = githubLink; }
    public String getLiveDemoLink() { return liveDemoLink; }
    public void setLiveDemoLink(String liveDemoLink) { this.liveDemoLink = liveDemoLink; }
    public String getTechStack() { return techStack; }
    public void setTechStack(String techStack) { this.techStack = techStack; }
    public SoftwareCategory getCategory() { return category; }
    public void setCategory(SoftwareCategory category) { this.category = category; }
    public SoftwarePricingDemand getPricingDemand() { return pricingDemand; }
    public void setPricingDemand(SoftwarePricingDemand pricingDemand) { this.pricingDemand = pricingDemand; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public SoftwareStatus getSoftwareStatus() { return softwareStatus; }
    public void setSoftwareStatus(SoftwareStatus softwareStatus) { this.softwareStatus = softwareStatus; }
    public SoftwarePurchaseType getPurchaseType() { return purchaseType; }
    public void setPurchaseType(SoftwarePurchaseType purchaseType) { this.purchaseType = purchaseType; }
    public boolean isStatus() { return status; }
    public void setStatus(boolean status) { this.status = status; }
    public long getViews() { return views; }
    public void setViews(long views) { this.views = views; }
    public Agreement getAgreement() { return agreement; }
    public void setAgreement(Agreement agreement) { this.agreement = agreement; }
    public AppUser getListedBy() { return listedBy; }
    public void setListedBy(AppUser listedBy) { this.listedBy = listedBy; }
    public List<SoftwareView> getSoftwareViews() { return softwareViews; }
    public void setSoftwareViews(List<SoftwareView> softwareViews) { this.softwareViews = softwareViews; }
    public Long getVersion() { return version; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void setVersion(Long version) {
        this.version = version;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isOfficial() {
        return official;
    }

    public void setOfficial(boolean official) {
        this.official = official;
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

    public long getPurchaseCount() {
        return purchaseCount;
    }

    public void setPurchaseCount(long purchaseCount) {
        this.purchaseCount = purchaseCount;
    }

    public List<SoftwarePurchase> getPurchases() {
        return purchases;
    }

    public void setPurchases(List<SoftwarePurchase> purchases) {
        this.purchases = purchases;
    }
}