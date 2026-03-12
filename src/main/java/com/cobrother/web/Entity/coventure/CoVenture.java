package com.cobrother.web.Entity.coventure;

import com.cobrother.web.Entity.user.AppUser;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * CoVenture with a UNIQUE constraint on (venture_id, applicant_user_id).
 * Three-layer duplicate prevention:
 *  1. Frontend: checks /my-status before showing form
 *  2. Backend controller: existsByVentureIdAndApplicantId check before insert
 *  3. Database: UniqueConstraint rejects concurrent duplicate inserts
 */
@Entity
@Table(
        name = "co_venture",
        uniqueConstraints = {
                @UniqueConstraint(
                        name        = "uq_coventure_venture_applicant",
                        columnNames = { "venture_id", "applicant_user_id" }
                )
        }
)
public class CoVenture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fullName;
    private String phone;
    private String location;
    private String gstNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CoVentureStatus status = CoVentureStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venture_id", nullable = false)
    @JsonIgnoreProperties({
            "coVentureApplications", "listedBy", "purchasedBy",
            "hibernateLazyInitializer", "handler"
    })
    private Venture venture;

    // Add after private String gstNo;:
    @Column(columnDefinition = "TEXT")
    private String description;   // how the applicant can help

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

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


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applicant_user_id", nullable = false)
    @JsonIgnoreProperties({
            "listedDomains", "purchasedDomains", "listedVentures", "purchasedVentures",
            "coVenturedVentures", "communityProfile", "password", "otp", "otpExpiry",
            "emailOtp", "emailOtpExpiry", "verificationToken", "verificationTokenExpiry",
            "refreshToken", "hibernateLazyInitializer", "handler"
    })
    private AppUser applicant;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getGstNo() { return gstNo; }
    public void setGstNo(String gstNo) { this.gstNo = gstNo; }

    public CoVentureStatus getStatus() { return status; }
    public void setStatus(CoVentureStatus status) { this.status = status; }

    public Venture getVenture() { return venture; }
    public void setVenture(Venture venture) { this.venture = venture; }

    public AppUser getApplicant() { return applicant; }
    public void setApplicant(AppUser applicant) { this.applicant = applicant; }


    public String getDescription() { return description; }
    public void setDescription(String v) { this.description = v; }

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
}