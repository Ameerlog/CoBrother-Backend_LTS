package com.cobrother.web.Entity.coventure;

import com.cobrother.web.Entity.user.AppUser;
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

    // Seller
    @ManyToOne
    @JoinColumn(name = "listed_by_user_id")
    private AppUser listedBy;

    // Buyer
    @ManyToOne
    @JoinColumn(name = "purchased_by_user_id")
    private AppUser purchasedBy;

    // All co-venture applications for this venture
    @OneToMany(mappedBy = "venture", cascade = CascadeType.ALL, orphanRemoval = true)
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
    public void setCoVentureApplicationCount(long coVentureApplicationCount) {
        this.coVentureApplicationCount = coVentureApplicationCount;
    }

    public List<CoVenture> getCoVentureApplications() { return coVentureApplications; }
    public void setCoVentureApplications(List<CoVenture> coVentureApplications) {
        this.coVentureApplications = coVentureApplications;
    }
}