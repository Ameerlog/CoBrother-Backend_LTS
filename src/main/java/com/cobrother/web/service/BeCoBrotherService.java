package com.cobrother.web.service;

import com.cobrother.web.Repository.BeCoBrotherRepository;
import com.cobrother.web.model.becobrother.BeCobrother;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BeCoBrotherService {

    @Autowired
    private BeCoBrotherRepository beCoBrotherRepository;

    public BeCobrother joiningRequest(BeCobrother beCobrother) {
        return beCoBrotherRepository.save(beCobrother);
    }
}
