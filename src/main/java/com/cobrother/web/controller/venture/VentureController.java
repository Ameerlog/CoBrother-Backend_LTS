package com.cobrother.web.controller.venture;

import com.cobrother.web.Entity.coventure.Venture;
import com.cobrother.web.model.venture.VentureDto;
import com.cobrother.web.service.auth.CurrentUserService;
import com.cobrother.web.service.venture.VentureService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("api/v1/venture")
public class VentureController {

    @Autowired
    public VentureService ventureService;

    @Autowired
    private CurrentUserService currentUserService;

    /**
     * GET /api/v1/venture/all
     * Returns all ventures as safe DTOs (no circular references)
     */
    @GetMapping("/all")
    public ResponseEntity<List<VentureDto>> getAllVentures() {
        List<Venture> ventures = ventureService.getAllVentures();
        List<VentureDto> dtos = ventures.stream()
                .map(VentureDto::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /**
     * GET /api/v1/venture/my
     * Returns ventures listed by the current user
     */
    @GetMapping("/my")
    public ResponseEntity<List<VentureDto>> getMyVentures() {
        List<Venture> ventures = ventureService.getVenturesByUser(currentUserService.getCurrentUser());
        List<VentureDto> dtos = ventures.stream()
                .map(VentureDto::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /**
     * GET /api/v1/venture/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<VentureDto> getVenture(@PathVariable long id) {
        Venture v = ventureService.getVentureEntity(id);
        return ResponseEntity.ok(VentureDto.from(v));
    }

    /**
     * POST /api/v1/venture
     */
    @PostMapping
    public ResponseEntity<VentureDto> addVenture(@RequestBody Venture venture) {
        venture.setListedBy(currentUserService.getCurrentUser());
        Venture saved = ventureService.addVentureEntity(venture);
        return ResponseEntity.ok(VentureDto.from(saved));
    }

    /**
     * PUT /api/v1/venture/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<VentureDto> updateVenture(@PathVariable long id, @RequestBody Venture venture) {
        Venture updated = ventureService.updateVentureEntity(id, venture);
        return ResponseEntity.ok(VentureDto.from(updated));
    }

    /**
     * DELETE /api/v1/venture/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVenture(@PathVariable long id) {
        ventureService.deleteVenture(id);
        return ResponseEntity.noContent().build();
    }
}