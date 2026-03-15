package com.cobrother.web.controller.cobrother;

import com.cobrother.web.service.auth.CurrentUserService;
import com.cobrother.web.service.cobrother.CoBrotherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("api/v1/cobrother")
@PreAuthorize("hasRole('COBROTHER')")
public class CoBrotherController {

    @Autowired private CoBrotherService coBrotherService;
    @Autowired private CurrentUserService currentUserService;

    @GetMapping("/requests")
    public ResponseEntity<?> getMyRequests() {
        return coBrotherService.getMyRequests(currentUserService.getCurrentUser());
    }

    @PutMapping("/requests/{id}/respond")
    public ResponseEntity<?> respond(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        boolean accepted = Boolean.parseBoolean(body.get("accepted").toString());
        String note = body.getOrDefault("note", "").toString();
        return coBrotherService.respond(id, accepted, note, currentUserService.getCurrentUser());
    }
}