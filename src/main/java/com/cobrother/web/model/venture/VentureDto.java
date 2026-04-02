package com.cobrother.web.model.venture;

import com.cobrother.web.Entity.coventure.*;
import com.cobrother.web.Entity.user.AppUser;
import com.cobrother.web.Entity.user.UserRole;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Safe DTO for Venture responses.
 * Roles are returned as a typed list — no JSON string parsing on the frontend.
 */
public class VentureDto {

    private Long id;
    private BrandDetails brandDetails;
    private ContactInfo contactInfo;
    private Agreement agreement;
    private boolean status;
    private long views;
    private long coVentureApplicationCount;

    private UserSummary listedBy;
    private UserSummary purchasedBy;

    private VentureStage stage;
    private boolean canEdit;
    private List<VentureRoleDto> roles;   // ← typed list, replaces lookingFor string
    private String currentProblem;

    // ── Nested DTOs ───────────────────────────────────────────────────────────
    public static class UserSummary {
        public Long id;
        public String email;
        public String firstname;
        public String lastname;
    }

    public static class VentureRoleDto {
        public Long   id;
        public String type;
        public String title;
        public String skillDomain;
        public String description;
        public String commitment;
        public String location;
        public String experienceLevel;
        public Double equityMin;
        public Double equityMax;
        public String vestingTerms;
        public Double salaryMin;
        public Double salaryMax;
        public Double budgetMin;
        public Double budgetMax;
        public Double investmentMin;
        public Double investmentMax;
        public int    sortOrder;

        public static VentureRoleDto from(VentureRole r) {
            VentureRoleDto dto = new VentureRoleDto();
            dto.id              = r.getId();
            dto.type            = r.getType();
            dto.title           = r.getTitle();
            dto.skillDomain     = r.getSkillDomain();
            dto.description     = r.getDescription();
            dto.commitment      = r.getCommitment();
            dto.location        = r.getLocation();
            dto.experienceLevel = r.getExperienceLevel();
            dto.equityMin       = r.getEquityMin();
            dto.equityMax       = r.getEquityMax();
            dto.vestingTerms    = r.getVestingTerms();
            dto.salaryMin       = r.getSalaryMin();
            dto.salaryMax       = r.getSalaryMax();
            dto.budgetMin       = r.getBudgetMin();
            dto.budgetMax       = r.getBudgetMax();
            dto.investmentMin   = r.getInvestmentMin();
            dto.investmentMax   = r.getInvestmentMax();
            dto.sortOrder       = r.getSortOrder();
            return dto;
        }

        /** Map DTO back to entity (venture link set by the service) */
        public VentureRole toEntity() {
            VentureRole role = new VentureRole();
            role.setType(this.type);
            role.setTitle(this.title);
            role.setSkillDomain(this.skillDomain);
            role.setDescription(this.description);
            role.setCommitment(this.commitment);
            role.setLocation(this.location);
            role.setExperienceLevel(this.experienceLevel);
            role.setEquityMin(this.equityMin);
            role.setEquityMax(this.equityMax);
            role.setVestingTerms(this.vestingTerms);
            role.setSalaryMin(this.salaryMin);
            role.setSalaryMax(this.salaryMax);
            role.setBudgetMin(this.budgetMin);
            role.setBudgetMax(this.budgetMax);
            role.setInvestmentMin(this.investmentMin);
            role.setInvestmentMax(this.investmentMax);
            role.setSortOrder(this.sortOrder);
            return role;
        }
    }

    // ── Factory ───────────────────────────────────────────────────────────────
    public static VentureDto from(Venture v, AppUser currentUser) {
        VentureDto dto = new VentureDto();
        dto.id                        = v.getId();
        dto.brandDetails              = v.getBrandDetails();
        dto.contactInfo               = v.getContactInfo();
        dto.agreement                 = v.getAgreement();
        dto.status                    = v.isStatus();
        dto.views                     = v.getViews();
        dto.coVentureApplicationCount = v.getCoVentureApplicationCount();
        dto.stage                     = v.getStage();
        dto.currentProblem            = v.getCurrentProblem();

        // Calculate canEdit: true if current user is ADMIN or owner
        if (currentUser != null && v.getListedBy() != null) {
            boolean isAdmin = currentUser.getRole() == UserRole.ADMIN;
            boolean isOwner = v.getListedBy().getId().equals(currentUser.getId());
            dto.canEdit = isAdmin || isOwner;
        } else {
            dto.canEdit = false;
        }

        dto.roles = v.getRoles() == null ? List.of() :
                v.getRoles().stream()
                        .map(VentureRoleDto::from)
                        .collect(Collectors.toList());

        if (v.getListedBy() != null) {
            UserSummary u = new UserSummary();
            u.id        = v.getListedBy().getId();
            u.email     = v.getListedBy().getEmail();
            u.firstname = v.getListedBy().getFirstname();
            u.lastname  = v.getListedBy().getLastname();
            dto.listedBy = u;
        }
        if (v.getPurchasedBy() != null) {
            UserSummary u = new UserSummary();
            u.id        = v.getPurchasedBy().getId();
            u.email     = v.getPurchasedBy().getEmail();
            u.firstname = v.getPurchasedBy().getFirstname();
            u.lastname  = v.getPurchasedBy().getLastname();
            dto.purchasedBy = u;
        }
        return dto;
    }

    public static VentureDto from(Venture v) {
        VentureDto dto = new VentureDto();
        dto.id                        = v.getId();
        dto.brandDetails              = v.getBrandDetails();
        dto.contactInfo               = v.getContactInfo();
        dto.agreement                 = v.getAgreement();
        dto.status                    = v.isStatus();
        dto.views                     = v.getViews();
        dto.coVentureApplicationCount = v.getCoVentureApplicationCount();
        dto.stage                     = v.getStage();
        dto.currentProblem            = v.getCurrentProblem();

        dto.roles = v.getRoles() == null ? List.of() :
                v.getRoles().stream()
                        .map(VentureRoleDto::from)
                        .collect(Collectors.toList());

        if (v.getListedBy() != null) {
            UserSummary u = new UserSummary();
            u.id        = v.getListedBy().getId();
            u.email     = v.getListedBy().getEmail();
            u.firstname = v.getListedBy().getFirstname();
            u.lastname  = v.getListedBy().getLastname();
            dto.listedBy = u;
        }
        if (v.getPurchasedBy() != null) {
            UserSummary u = new UserSummary();
            u.id        = v.getPurchasedBy().getId();
            u.email     = v.getPurchasedBy().getEmail();
            u.firstname = v.getPurchasedBy().getFirstname();
            u.lastname  = v.getPurchasedBy().getLastname();
            dto.purchasedBy = u;
        }
        return dto;
    }

    /** Map DTO → Venture entity (roles linked in VentureService) */
    public Venture toEntity() {
        Venture v = new Venture();
        v.setBrandDetails(this.brandDetails);
        v.setContactInfo(this.contactInfo);
        v.setAgreement(this.agreement);
        v.setStatus(this.status);
        v.setStage(this.stage);
        v.setCurrentProblem(this.currentProblem);
        // Roles are NOT set here — VentureService handles the cascade
        return v;
    }

    // ── Getters ───────────────────────────────────────────────────────────────
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
    public List<VentureRoleDto> getRoles() { return roles; }
    public String getCurrentProblem() { return currentProblem; }
    public boolean isCanEdit() { return canEdit; }
}