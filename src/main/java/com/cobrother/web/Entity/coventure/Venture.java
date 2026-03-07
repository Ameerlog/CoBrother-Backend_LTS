package com.cobrother.web.Entity.coventure;

import com.cobrother.web.Entity.user.AppUser;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
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

    // Only expose safe user fields — never the full graph
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
    private VentureStage stage;          // IDEA, MVP, REVENUE_GENERATING, SCALING

    private String lookingFor;           // free text: "Marketing co-founder, Angel investor"

    @Column(columnDefinition = "TEXT")
    private String currentProblem;

    // Break the back-reference loop: don't serialize applications from Venture side
    @OneToMany(mappedBy = "venture", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties("venture")
    private List<CoVenture> coVentureApplications = new ArrayList<>();

    // ── Getters & Setters ────────────────────────────────────────────────────

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
    public void setCoVentureApplications(List<CoVenture> coVentureApplications) {
        this.coVentureApplications = coVentureApplications;
    }

    public String getCurrentProblem() {
        return currentProblem;
    }

    public void setCurrentProblem(String currentProblem) {
        this.currentProblem = currentProblem;
    }

    public String getLookingFor() {
        return lookingFor;
    }

    public void setLookingFor(String lookingFor) {
        this.lookingFor = lookingFor;
    }

    public VentureStage getStage() {
        return stage;
    }

    public void setStage(VentureStage stage) {
        this.stage = stage;
    }
}