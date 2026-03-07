package com.cobrother.web.model.community;

import com.cobrother.web.Entity.community.CommunityIndustry;
import com.cobrother.web.Entity.community.CommunityRole;

public class CommunityUpdateRequest {
    private CommunityRole role;
    private String skills;
    private CommunityIndustry industry;
    private String location;
    private String whyImHere;

    public CommunityRole     getRole()                    { return role; }
    public void              setRole(CommunityRole v)     { this.role = v; }
    public String            getSkills()                  { return skills; }
    public void              setSkills(String v)          { this.skills = v; }
    public CommunityIndustry getIndustry()                { return industry; }
    public void              setIndustry(CommunityIndustry v){ this.industry = v; }
    public String            getLocation()                { return location; }
    public void              setLocation(String v)        { this.location = v; }

    public String getWhyImHere() {
        return whyImHere;
    }

    public void setWhyImHere(String whyImHere) {
        this.whyImHere = whyImHere;
    }
}