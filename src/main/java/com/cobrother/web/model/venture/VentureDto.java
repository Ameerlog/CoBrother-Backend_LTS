package com.cobrother.web.model.venture;

import com.cobrother.web.Entity.coventure.*;

/**
 * Safe DTO for Venture responses.
 * Avoids circular JSON serialization entirely.
 */
public class VentureDto {

    private Long id;
    private BrandDetails brandDetails;
    private ContactInfo contactInfo;
    private Agreement agreement;
    private boolean status;
    private long views;
    private long coVentureApplicationCount;

    // Only safe user fields
    private UserSummary listedBy;
    private UserSummary purchasedBy;


    private VentureStage stage;
    private String lookingFor;
    private String currentProblem;


    public static class UserSummary {
        public Long id;
        public String email;
        public String firstname;
        public String lastname;
    }

    public static VentureDto from(com.cobrother.web.Entity.coventure.Venture v) {
        VentureDto dto = new VentureDto();
        dto.id = v.getId();
        dto.brandDetails = v.getBrandDetails();
        dto.contactInfo = v.getContactInfo();
        dto.agreement = v.getAgreement();
        dto.status = v.isStatus();
        dto.views = v.getViews();
        dto.coVentureApplicationCount = v.getCoVentureApplicationCount();

        dto.stage = v.getStage();
        dto.lookingFor = v.getLookingFor();
        dto.currentProblem = v.getCurrentProblem();

        if (v.getListedBy() != null) {
            UserSummary u = new UserSummary();
            u.id = v.getListedBy().getId();
            u.email = v.getListedBy().getEmail();
            u.firstname = v.getListedBy().getFirstname();
            u.lastname = v.getListedBy().getLastname();
            dto.listedBy = u;
        }
        if (v.getPurchasedBy() != null) {
            UserSummary u = new UserSummary();
            u.id = v.getPurchasedBy().getId();
            u.email = v.getPurchasedBy().getEmail();
            u.firstname = v.getPurchasedBy().getFirstname();
            u.lastname = v.getPurchasedBy().getLastname();
            dto.purchasedBy = u;
        }
        return dto;
    }

    public Long getId() { return id; }
    public BrandDetails getBrandDetails() { return brandDetails; }
    public ContactInfo getContactInfo() { return contactInfo; }
    public Agreement getAgreement() { return agreement; }
    public boolean isStatus() { return status; }
    public long getViews() { return views; }
    public long getCoVentureApplicationCount() { return coVentureApplicationCount; }
    public UserSummary getListedBy() { return listedBy; }
    public UserSummary getPurchasedBy() { return purchasedBy; }

    public VentureStage getStage() { return stage; }
    public String getLookingFor() { return lookingFor; }
    public String getCurrentProblem() { return currentProblem; }

}