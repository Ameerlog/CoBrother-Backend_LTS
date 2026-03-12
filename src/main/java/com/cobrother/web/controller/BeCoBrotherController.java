package com.cobrother.web.controller;

import com.cobrother.web.model.becobrother.BeCobrother;
import com.cobrother.web.service.BeCoBrotherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/becobrother")
public class BeCoBrotherController {

    @Autowired
    public BeCoBrotherService beCoBrotherService;

    @PostMapping
    public BeCobrother joiningRequest(BeCobrother beCobrother) {
        return beCoBrotherService.joiningRequest(beCobrother);
    }
}
