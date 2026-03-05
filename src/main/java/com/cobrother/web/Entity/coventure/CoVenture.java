package com.cobrother.web.Entity.coventure;

import com.cobrother.web.Entity.user.AppUser;
import jakarta.persistence.*;

@Entity
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

    // The venture this application is for
    @ManyToOne
    @JoinColumn(name = "venture_id", nullable = false)
    private Venture venture;

    // The user who applied
    @ManyToOne
    @JoinColumn(name = "applicant_user_id", nullable = false)
    private AppUser applicant;

    // ── Getters & Setters ────────────────────────────────────────────────────

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
}