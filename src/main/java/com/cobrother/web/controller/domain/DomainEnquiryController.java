package com.cobrother.web.controller.domain;

import com.cobrother.web.Entity.cobranding.Domain;
import com.cobrother.web.Entity.cobranding.DomainEnquiry;
import com.cobrother.web.Entity.user.AppUser;
import com.cobrother.web.Repository.DomainEnquiryRepository;
import com.cobrother.web.Repository.DomainRepository;
import com.cobrother.web.service.auth.CurrentUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("api/v1/domain-enquiry")
public class DomainEnquiryController {

    @Autowired private DomainEnquiryRepository enquiryRepository;
    @Autowired private DomainRepository domainRepository;
    @Autowired private CurrentUserService currentUserService;

    @PostMapping("/{domainId}")
    public ResponseEntity<?> submitEnquiry(
            @PathVariable Long domainId,
            @RequestBody Map<String, String> body) {
        try {
            AppUser enquirer = currentUserService.getCurrentUser();
            Domain domain = domainRepository.getDomainById(domainId);

            if (domain == null)
                return ResponseEntity.notFound().build();
            if (domain.getListedBy().getId().equals(enquirer.getId()))
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "You cannot enquire about your own domain."));
            if (domain.getAskingPrice() < 500000)
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Enquire Now is only for domains above ₹5,00,000."));
            if (enquiryRepository.existsByDomainIdAndEnquirerId(domainId, enquirer.getId()))
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "You have already submitted an enquiry for this domain."));

            DomainEnquiry enquiry = new DomainEnquiry();
            enquiry.setDomain(domain);
            enquiry.setEnquirer(enquirer);
            enquiry.setFullName(body.get("fullName"));
            enquiry.setEmail(body.get("email"));
            enquiry.setPhone(body.get("phone"));
            enquiry.setMessage(body.get("message"));

            return ResponseEntity.ok(enquiryRepository.save(enquiry));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }
}