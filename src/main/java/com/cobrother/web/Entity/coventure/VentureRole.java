package com.cobrother.web.Entity.coventure;

import jakarta.persistence.*;

@Entity
@Table(name = "venture_role")
public class VentureRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Relationship ──────────────────────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venture_id", nullable = false)
    private Venture venture;

    // ── Core fields ───────────────────────────────────────────────────────────
    @Column(nullable = false, length = 50)
    private String type;             // COFOUNDER | SALARIED | FREELANCE | INVESTOR

    @Column(nullable = false, length = 120)
    private String title;

    @Column(length = 100)
    private String skillDomain;

    @Column(columnDefinition = "TEXT")
    private String description;

    // ── Work arrangement ──────────────────────────────────────────────────────
    @Column(length = 30)
    private String commitment;       // FULL_TIME | PART_TIME | PROJECT_BASED

    @Column(length = 20)
    private String location;         // REMOTE | ONSITE | HYBRID

    @Column(length = 20)
    private String experienceLevel;  // ANY | JUNIOR | MID | SENIOR

    // ── Compensation (nullable — depends on type) ─────────────────────────────
    private Double equityMin;
    private Double equityMax;
    private String vestingTerms;

    private Double salaryMin;
    private Double salaryMax;

    private Double budgetMin;
    private Double budgetMax;

    private Double investmentMin;
    private Double investmentMax;

    // ── Ordering ──────────────────────────────────────────────────────────────
    @Column(nullable = false, columnDefinition = "int default 0")
    private int sortOrder = 0;

    // ─── Getters & Setters ────────────────────────────────────────────────────
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Venture getVenture() { return venture; }
    public void setVenture(Venture venture) { this.venture = venture; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSkillDomain() { return skillDomain; }
    public void setSkillDomain(String skillDomain) { this.skillDomain = skillDomain; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCommitment() { return commitment; }
    public void setCommitment(String commitment) { this.commitment = commitment; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getExperienceLevel() { return experienceLevel; }
    public void setExperienceLevel(String experienceLevel) { this.experienceLevel = experienceLevel; }

    public Double getEquityMin() { return equityMin; }
    public void setEquityMin(Double equityMin) { this.equityMin = equityMin; }

    public Double getEquityMax() { return equityMax; }
    public void setEquityMax(Double equityMax) { this.equityMax = equityMax; }

    public String getVestingTerms() { return vestingTerms; }
    public void setVestingTerms(String vestingTerms) { this.vestingTerms = vestingTerms; }

    public Double getSalaryMin() { return salaryMin; }
    public void setSalaryMin(Double salaryMin) { this.salaryMin = salaryMin; }

    public Double getSalaryMax() { return salaryMax; }
    public void setSalaryMax(Double salaryMax) { this.salaryMax = salaryMax; }

    public Double getBudgetMin() { return budgetMin; }
    public void setBudgetMin(Double budgetMin) { this.budgetMin = budgetMin; }

    public Double getBudgetMax() { return budgetMax; }
    public void setBudgetMax(Double budgetMax) { this.budgetMax = budgetMax; }

    public Double getInvestmentMin() { return investmentMin; }
    public void setInvestmentMin(Double investmentMin) { this.investmentMin = investmentMin; }

    public Double getInvestmentMax() { return investmentMax; }
    public void setInvestmentMax(Double investmentMax) { this.investmentMax = investmentMax; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}