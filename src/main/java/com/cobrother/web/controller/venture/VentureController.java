package com.cobrother.web.controller.venture;

import com.cobrother.web.Entity.coventure.Venture;
import com.cobrother.web.service.venture.VentureService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/v1/venture")
public class VentureController {

    @Autowired
    public VentureService ventureService;


    @GetMapping("/{id}")
    public ResponseEntity<Venture> getVenture(@PathVariable long id) {
        return ventureService.getVenture(id);
    }

    @PostMapping
    public ResponseEntity<Venture> addVenture(@RequestBody Venture venture) {
        return ventureService.addVenture(venture);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Venture> updateVenture(@PathVariable long id, @RequestBody Venture venture) {
        return ventureService.updateVenture(id, venture);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Venture> deleteVenture(@PathVariable long id) {
        return ventureService.deleteVenture(id);
    }

//    @GetMapping("/all")
//    public ResponseEntity<List<Venture>> getAllVenture() {
//        return ventureService.getAllVenture();
//    }

}
