package com.cobrother.web.Entity.coventure;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Embeddable
public class BrandDetails{
    private String description;
    private String brandName;
    private String website;
    private String videoUrl;
    @Enumerated(EnumType.STRING)
    private Industry industry;
    private Long dealValue;        // ₹5,00,000
    private String logoUrl;
    @Enumerated(EnumType.STRING)
    public VentureType ventureType;

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    //
    public String getBrandName() {
        return brandName;
    }

    public void setBrandName(String brandName) {
        this.brandName = brandName;
    }

    public VentureType getVentureType() {
        return ventureType;
    }

    public void setVentureType(VentureType ventureType) {
        this.ventureType = ventureType;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public Industry getIndustry() {
        return industry;
    }

    public void setIndustry(Industry industry) {
        this.industry = industry;
    }

    public Long getDealValue() {
        return dealValue;
    }

    public void setDealValue(Long dealValue) {
        this.dealValue = dealValue;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }


}