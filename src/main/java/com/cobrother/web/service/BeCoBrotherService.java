package com.cobrother.web.service;

import com.cobrother.web.Repository.BeCoBrotherRepository;
import com.cobrother.web.model.becobrother.BeCobrother;
import com.cobrother.web.service.auth.JoinUsMailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BeCoBrotherService {

    @Autowired
    private BeCoBrotherRepository beCoBrotherRepository;

    @Autowired
    private JoinUsMailService joinUsMailService;

    public BeCobrother joiningRequest(BeCobrother beCobrother) {
        BeCobrother saved = beCoBrotherRepository.save(beCobrother);
        
        joinUsMailService.sendJoinUsEmail(
                saved.getFullName() != null ? saved.getFullName() : "N/A",
                saved.getEmail() != null ? saved.getEmail() : "N/A",
                saved.getPhoneNumber() != null ? saved.getPhoneNumber() : "N/A",
                saved.getSkill() != null ? String.valueOf(saved.getSkill()) : "N/A",
                saved.getPinCode() != null ? saved.getPinCode() : "N/A",
                saved.isEquipment() ? "Yes" : "No"
        );
        
        return saved;
    }
}
