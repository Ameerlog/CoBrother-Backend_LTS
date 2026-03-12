package com.cobrother.web.controller.domain;

import com.cobrother.web.Entity.cobranding.Domain;
import com.cobrother.web.Entity.user.AppUser;
import com.cobrother.web.service.auth.CurrentUserService;
import com.cobrother.web.service.domain.DomainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("api/v1/domain")
public class DomainController {

    @Autowired
    private DomainService domainService;

    @Autowired
    private CurrentUserService currentUserService;

    @GetMapping("/all")
    public ResponseEntity<List<Domain>> getAllDomains() {
        return domainService.getAllDomains();
    }

    @GetMapping("/my-listings")
    public ResponseEntity<List<Domain>> getMyListings() {
        return domainService.getMyListedDomains(currentUserService.getCurrentUser());
    }

    @GetMapping("/my-purchases")
    public ResponseEntity<List<Domain>> getMyPurchases() {
        return domainService.getMyPurchasedDomains(currentUserService.getCurrentUser());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Domain> getDomain(@PathVariable long id) {
        return domainService.getDomain(id);
    }

    @PostMapping
    public ResponseEntity<Domain> addDomain(@RequestBody Domain domain) {
        domain.setListedBy(currentUserService.getCurrentUser());
        return domainService.addDomain(domain);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Domain> updateDomain(@PathVariable long id, @RequestBody Domain domain) {

        return domainService.updateDomain(id, domain, currentUserService.getCurrentUser());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Domain> deleteDomain(@PathVariable long id) {
        return domainService.deleteDomain(id, currentUserService.getCurrentUser());
    }

    // ── Payment endpoints ─────────────────────────────────────────────────────

    @PostMapping("/{id}/purchase/create-order")
    public ResponseEntity<?> createOrder(@PathVariable long id) {
        AppUser buyer = currentUserService.getCurrentUser();
        return domainService.createPurchaseOrder(id, buyer);
    }

    @PostMapping("/{id}/purchase/verify")
    public ResponseEntity<?> verifyPayment(
            @PathVariable long id,
            @RequestBody Map<String, String> payload) {
        AppUser buyer = currentUserService.getCurrentUser();
        return domainService.verifyPayment(
                id,
                payload.get("razorpayPaymentId"),
                payload.get("razorpayOrderId"),
                payload.get("razorpaySignature"),
                buyer
        );
    }

    @PostMapping("/{id}/purchase/failure")
    public ResponseEntity<?> handleFailure(@PathVariable long id) {
        return domainService.handlePaymentFailure(id);
    }
}