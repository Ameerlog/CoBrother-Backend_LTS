package com.cobrother.web.model.community;

import com.cobrother.web.Entity.community.CommunityIndustry;
import com.cobrother.web.Entity.community.CommunityRole;

public class CommunityUpdateRequest {

    private CommunityRole role;
    private String skills;          // comma-separated e.g. "Java,Spring,React"
    private CommunityIndustry industry;
    private String location;

    public CommunityRole getRole() { return role; }
    public void setRole(CommunityRole role) { this.role = role; }

    public String getSkills() { return skills; }
    public void setSkills(String skills) { this.skills = skills; }

    public CommunityIndustry getIndustry() { return industry; }
    public void setIndustry(CommunityIndustry industry) { this.industry = industry; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
}
