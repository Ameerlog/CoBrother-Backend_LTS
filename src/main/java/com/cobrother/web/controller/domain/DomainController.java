package com.cobrother.web.controller.domain;

import com.cobrother.web.Entity.cobranding.Domain;
import com.cobrother.web.Entity.coventure.Venture;
import com.cobrother.web.service.domain.DomainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/domain")
public class DomainController {

    @Autowired
    private DomainService domainService;

    @GetMapping("/{id}")
    public ResponseEntity<Domain> getDomain(@PathVariable long id) {
        return domainService.getDomain(id);
    }

    @PostMapping
    public ResponseEntity<Domain> addDomain(@RequestBody Domain domain) {
        return domainService.addDomain(domain);
    }


    @PutMapping("/{id}")
    public ResponseEntity<Domain> updatedomain(@PathVariable long id, @RequestBody Domain domain) {
        return domainService.updatedomain(id, domain);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Domain> deleteDomain(@PathVariable long id) {
        return domainService.deleteDomain(id);
    }
}
