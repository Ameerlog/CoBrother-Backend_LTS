package com.cobrother.web.service.domain;

import com.cobrother.web.Entity.cobranding.Domain;
import com.cobrother.web.Repository.DomainRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class DomainService {

    @Autowired
    private DomainRepository domainRepository;

    public ResponseEntity<Domain> getDomain(long id) {
        try{
            return ResponseEntity.ok(domainRepository.getDomainById(id));
        }
        catch(Exception e){
            e.printStackTrace();
        }

        return ResponseEntity.notFound().build();
    }

    public ResponseEntity<Domain> addDomain(Domain domain) {
        try {
            return ResponseEntity.ok(domainRepository.save(domain));
        }
        catch(Exception e){
            e.printStackTrace();
        }
        return ResponseEntity.badRequest().build();
    }


    public ResponseEntity<Domain> updatedomain(long id, Domain domain) {
        try {
            Domain updatedDomain = domainRepository.getDomainById(id);
            if(updatedDomain != null){
                updatedDomain.setDomainCategory(domain.getDomainCategory());
                updatedDomain.setDomainName(domain.getDomainName());
                updatedDomain.setDomainExtension(domain.getDomainExtension());
                updatedDomain.setContactInfo(domain.getContactInfo());
                updatedDomain.setAskingPrice(domain.getAskingPrice());
                return ResponseEntity.ok(domainRepository.save(updatedDomain));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.badRequest().build();
    }

    public ResponseEntity<Domain> deleteDomain(long id) {
        try {
            Domain domain = domainRepository.getDomainById(id);
            domain.setStatus(false);
            return ResponseEntity.ok(domainRepository.save(domain));
        }
        catch(Exception e){
            e.printStackTrace();
        }
        return ResponseEntity.badRequest().build();
    }
}
