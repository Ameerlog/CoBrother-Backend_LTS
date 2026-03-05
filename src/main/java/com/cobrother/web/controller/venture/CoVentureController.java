package com.cobrother.web.controller.venture;

import com.cobrother.web.Entity.coventure.CoVenture;
import com.cobrother.web.service.venture.CoVentureService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/coventure")
public class CoVentureController {

    @Autowired
    private CoVentureService coVentureService;

    @PostMapping("/{ventureId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> applyToCoVenture(
            @PathVariable long ventureId,
            @RequestBody CoVenture application) {
        return coVentureService.applyToCoVenture(ventureId, application);
    }
}