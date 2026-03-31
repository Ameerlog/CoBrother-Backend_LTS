package com.cobrother.web.controller;

import com.cobrother.web.Entity.cobranding.Domain;
import com.cobrother.web.Entity.cocreation.Software;
import com.cobrother.web.Entity.coventure.Venture;
import com.cobrother.web.Entity.user.UserRole;
import com.cobrother.web.Repository.DomainRepository;
import com.cobrother.web.Repository.SoftwareRepository;
import com.cobrother.web.Repository.VentureRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("public/api/v1")
public class PublicController {

    @Autowired
    private DomainRepository domainRepository;

    @Autowired
    private SoftwareRepository softwareRepository;

    @Autowired
    private VentureRepository ventureRepository;

    @GetMapping("domains")
    public List<Domain> getDomains() {
        return domainRepository.findAllDomainsByListedByCobrother(UserRole.ADMIN);
    }

    @GetMapping("ventures")
    public List<Venture> getVentures() {
        return ventureRepository.findAllByVenturesListedByCobrother(UserRole.ADMIN);
    }
// added this line changess
   @GetMapping("softwares")
public List<Software> getSoftwares() {
    return softwareRepository.findAllSoftwareListedByCobrother(UserRole.ADMIN);
}
}
