package com.cobrother.web.controller.admin;

import com.cobrother.web.service.admin.AdminService;
import com.cobrother.web.service.auth.CurrentUserService;
import com.cobrother.web.service.cocreation.SoftwareService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Autowired private AdminService adminService;
    @Autowired private CurrentUserService currentUserService;
    @Autowired private SoftwareService softwareService;

    // ── View all applications ─────────────────────────────────────────────────
    @GetMapping("/coventures")
    public ResponseEntity<?> getAllCoVentures() {
        return adminService.getAllCoVentureApplications();
    }

    @GetMapping("/domains")
    public ResponseEntity<?> getAllDomains() {
        return adminService.getAllDomainPurchases();
    }

    @GetMapping("/cocreations")
    public ResponseEntity<?> getAllCoCreations() {
        return adminService.getAllCoCreationPurchases();
    }

    @GetMapping("/cobrother-requests")
    public ResponseEntity<?> getAllCoBrotherRequests() {
        return adminService.getAllCoBrotherRequests();
    }

    @GetMapping("/cobrothers")
    public ResponseEntity<?> getAllCoBrothers() {
        return adminService.getAllCoBrothers();
    }

    // ── Forward to CoBrother ──────────────────────────────────────────────────
    @PostMapping("/forward")
    public ResponseEntity<?> forward(@RequestBody Map<String, Object> body) {
        Long entityId    = Long.valueOf(body.get("entityId").toString());
        String type      = body.get("type").toString();
        Long coBrotherId = Long.valueOf(body.get("coBrotherId").toString());
        return adminService.forwardToCoBrother(entityId, type, coBrotherId,
                currentUserService.getCurrentUser());
    }

    // ── Admin lists official software ─────────────────────────────────────────
    @PostMapping("/cocreation")
    public ResponseEntity<?> listOfficialSoftware(
            @RequestBody com.cobrother.web.Entity.cocreation.Software software) {
        software.setListedBy(currentUserService.getCurrentUser());
        software.setOfficial(true); // marked as official
        return softwareService.create(software);
    }

    @GetMapping("/domain-enquiries")
    public ResponseEntity<?> getDomainEnquiries() {
        return adminService.getAllDomainEnquiries();
    }

    @PostMapping("/takedown")
    public ResponseEntity<?> takeDown(@RequestBody Map<String, String> body) {
        return adminService.takeDownListing(
                body.get("type"),
                Long.valueOf(body.get("entityId")),
                body.getOrDefault("reason", "")
        );
    }

    @PostMapping("/restore")
    public ResponseEntity<?> restore(@RequestBody Map<String, String> body) {
        return adminService.restoreListing(
                body.get("type"),
                Long.valueOf(body.get("entityId"))
        );
    }
}