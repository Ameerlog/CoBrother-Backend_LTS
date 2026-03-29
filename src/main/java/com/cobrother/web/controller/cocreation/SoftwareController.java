package com.cobrother.web.controller.cocreation;

import com.cobrother.web.Entity.cocreation.Software;
import com.cobrother.web.Entity.cocreation.SoftwarePurchase;
import com.cobrother.web.Entity.user.AppUser;
import com.cobrother.web.service.auth.CurrentUserService;
import com.cobrother.web.service.cocreation.SoftwareService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("api/v1/cocreation")
public class SoftwareController {

    @Autowired private SoftwareService softwareService;
    @Autowired private CurrentUserService currentUserService;

    @GetMapping("/all")
    public ResponseEntity<List<Software>> getAll() {
        AppUser viewer = currentUserService.getCurrentUser();
        return softwareService.getAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Software> getById(@PathVariable Long id) {
        return softwareService.getById(id, currentUserService.getCurrentUser());
    }

    @GetMapping("/my-listings")
    public ResponseEntity<List<Software>> getMyListings() {
        return softwareService.getMyListings(currentUserService.getCurrentUser());
    }

    @GetMapping("/my-purchases")
    public ResponseEntity<List<SoftwarePurchase>> getMyPurchases() {
        return softwareService.getMyPurchases(currentUserService.getCurrentUser());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Software> create(@RequestBody Software software) {
        software.setListedBy(currentUserService.getCurrentUser());
        return softwareService.create(software);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Software> update(@PathVariable Long id, @RequestBody Software software) {
        return softwareService.update(id, software, currentUserService.getCurrentUser());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Software> delete(@PathVariable Long id) {
        return softwareService.delete(id, currentUserService.getCurrentUser());
    }

    // Update existing createOrder endpoint to accept coBrotherOptIn:
    @PostMapping("/{id}/purchase/create-order")
    public ResponseEntity<?> createOrder(@PathVariable Long id,
                                         @RequestBody Map<String, Object> body) {
        AppUser buyer = currentUserService.getCurrentUser();
        boolean coBrotherOptIn = Boolean.TRUE.equals(body.get("coBrotherOptIn"));
        return softwareService.createOrder(id, buyer,
                (String) body.get("buyerFullName"),
                (String) body.get("buyerEmail"),
                (String) body.get("buyerPhone"),
                coBrotherOptIn);
    }

    // Add CoBrother help endpoints:
    @PostMapping("/purchase/{purchaseId}/cobrother-help/create-order")
    public ResponseEntity<?> createCoBrotherHelpOrder(@PathVariable Long purchaseId) {
        return softwareService.createCoBrotherHelpOrder(purchaseId,
                currentUserService.getCurrentUser());
    }

    @PostMapping("/purchase/{purchaseId}/cobrother-help/verify")
    public ResponseEntity<?> verifyCoBrotherHelp(@PathVariable Long purchaseId,
                                                 @RequestBody Map<String, String> body) {
        return softwareService.verifyCoBrotherHelp(purchaseId,
                currentUserService.getCurrentUser(),
                body.get("razorpayPaymentId"),
                body.get("razorpayOrderId"),
                body.get("razorpaySignature"));
    }

    @PostMapping("/{id}/purchase/verify")
    public ResponseEntity<?> verifyPayment(@PathVariable Long id,
                                           @RequestBody Map<String, String> body) {
        return softwareService.verifyPayment(id,
                body.get("razorpayPaymentId"),
                body.get("razorpayOrderId"),
                body.get("razorpaySignature"),
                currentUserService.getCurrentUser());
    }

    @PostMapping("/{id}/purchase/failure")
    public ResponseEntity<?> handleFailure(@PathVariable Long id) {
        return softwareService.handleFailure(id, currentUserService.getCurrentUser());
    }


    @GetMapping("/{id}/analytics")
    public ResponseEntity<Map<String, Object>> getAnalytics(@PathVariable Long id) {
        return softwareService.getAnalytics(id, currentUserService.getCurrentUser());
    }
}