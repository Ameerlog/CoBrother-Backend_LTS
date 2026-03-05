package com.cobrother.web.Entity.cobranding;

import com.cobrother.web.Entity.coventure.Agreement;
import com.cobrother.web.Entity.coventure.ContactInfo;
import com.cobrother.web.Entity.user.AppUser;
import jakarta.persistence.*;

@Entity
public class Domain {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String domainName;
    private String domainExtension;
    private String domainCategory;
    private double askingPrice;

    @Embedded
    private ContactInfo contactInfo;

    @Embedded
    private Agreement agreement;

    private String logo;

    private boolean status;
    private long views;

    // Seller
    @ManyToOne
    @JoinColumn(name = "listed_by_user_id")
    private AppUser listedBy;

    // Buyer
    @ManyToOne
    @JoinColumn(name = "purchased_by_user_id")
    private AppUser purchasedBy;

    // getters setters

    public Domain() {}


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public String getDomainExtension() {
        return domainExtension;
    }

    public void setDomainExtension(String domainExtension) {
        this.domainExtension = domainExtension;
    }

    public String getDomainCategory() {
        return domainCategory;
    }

    public void setDomainCategory(String domainCategory) {
        this.domainCategory = domainCategory;
    }

    public double getAskingPrice() {
        return askingPrice;
    }

    public void setAskingPrice(double askingPrice) {
        this.askingPrice = askingPrice;
    }

    public ContactInfo getContactInfo() {
        return contactInfo;
    }

    public void setContactInfo(ContactInfo contactInfo) {
        this.contactInfo = contactInfo;
    }

    public Agreement getAgreement() {
        return agreement;
    }

    public void setAgreement(Agreement agreement) {
        this.agreement = agreement;
    }

    public String getLogo() {
        return logo;
    }

    public void setLogo(String logo) {
        this.logo = logo;
    }

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public AppUser getListedBy() {
        return listedBy;
    }

    public void setListedBy(AppUser listedBy) {
        this.listedBy = listedBy;
    }

    public AppUser getPurchasedBy() {
        return purchasedBy;
    }

    public void setPurchasedBy(AppUser purchasedBy) {
        this.purchasedBy = purchasedBy;
    }

    public long getViews() {
        return views;
    }

    public void setViews(long views) {
        this.views = views;
    }
}