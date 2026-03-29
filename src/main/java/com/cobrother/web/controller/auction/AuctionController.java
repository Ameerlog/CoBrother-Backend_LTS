
package com.cobrother.web.controller.auction;

import com.cobrother.web.Entity.cobranding.AuctionDuration;
import com.cobrother.web.service.auction.AuctionService;
import com.cobrother.web.service.auth.CurrentUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("api/v1/auction")
public class AuctionController {

    @Autowired private AuctionService auctionService;
    @Autowired private CurrentUserService currentUserService;

    @PostMapping("/domain/{domainId}")
    public ResponseEntity<?> createAuction(
            @PathVariable Long domainId,
            @RequestBody Map<String, Object> body) {
        double minBid = ((Number) body.get("minBidPrice")).doubleValue();
        AuctionDuration duration = AuctionDuration.valueOf(body.get("duration").toString());
        return auctionService.createAuction(domainId, minBid, duration,
                currentUserService.getCurrentUser());
    }

    @GetMapping("/{auctionId}")
    public ResponseEntity<?> getAuction(@PathVariable Long auctionId) {
        return auctionService.getAuction(auctionId);
    }

    @GetMapping("/domain/{domainId}")
    public ResponseEntity<?> getAuctionByDomain(@PathVariable Long domainId) {
        return auctionService.getAuctionByDomain(domainId);
    }

    @PostMapping("/{auctionId}/bid")
    public ResponseEntity<?> placeBid(
            @PathVariable Long auctionId,
            @RequestBody Map<String, Object> body) {
        double amount = ((Number) body.get("amount")).doubleValue();
        return auctionService.placeBid(auctionId, amount,
                currentUserService.getCurrentUser());
    }

    @PostMapping("/{auctionId}/re-auction")
    public ResponseEntity<?> reAuction(
            @PathVariable Long auctionId,
            @RequestBody Map<String, Object> body) {
        double newMinBid = ((Number) body.get("minBidPrice")).doubleValue();
        AuctionDuration duration = AuctionDuration.valueOf(body.get("duration").toString());
        return auctionService.reAuction(auctionId, newMinBid, duration,
                currentUserService.getCurrentUser());
    }

    @PostMapping("/{auctionId}/close")
    public ResponseEntity<?> closeAuction(@PathVariable Long auctionId) {
        return auctionService.closeAuction(auctionId, currentUserService.getCurrentUser());
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllForAdmin() {
        return auctionService.getAllAuctionsForAdmin();
    }

    // Add to AuctionController:
    @GetMapping("/active")
    public ResponseEntity<?> getActiveAuctions() {
        return auctionService.getActiveAuctions();
    }
}
