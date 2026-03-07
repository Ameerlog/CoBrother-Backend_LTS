package com.cobrother.web.controller.analytics;

import com.cobrother.web.service.analytics.AnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

    @Autowired private AnalyticsService analyticsService;

    @GetMapping("/venture/{id}")
    public ResponseEntity<?> getVentureAnalytics(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(analyticsService.getVentureAnalytics(id));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfileAnalytics() {
        try {
            return ResponseEntity.ok(analyticsService.getProfileAnalytics());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }


}