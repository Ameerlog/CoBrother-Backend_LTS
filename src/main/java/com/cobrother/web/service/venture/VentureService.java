package com.cobrother.web.service.venture;

import com.cobrother.web.Entity.coventure.Venture;
import com.cobrother.web.Repository.VentureRepository;
import com.cobrother.web.service.auth.CurrentUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class VentureService {

    @Autowired
    private VentureRepository ventureRepository;

    @Autowired
    private CurrentUserService currentUserService;

    public ResponseEntity<Venture> getVenture(long id) {
        try{
            return ResponseEntity.ok(ventureRepository.getVentureById(id));
        }
        catch(Exception e){
            e.printStackTrace();
        }

        return ResponseEntity.notFound().build();
    }

    public ResponseEntity<Venture> addVenture(Venture venture) {
        try {
            venture.setListedBy(currentUserService.getCurrentUser());
            venture.setStatus(true);
            return ResponseEntity.ok(ventureRepository.save(venture));
        }
        catch(Exception e){
            e.printStackTrace();
        }
        return ResponseEntity.badRequest().build();
    }


    public ResponseEntity<Venture> updateVenture(long id, Venture venture) {
        Venture updatedVenture = ventureRepository.getVentureById(id);
        try {
            updatedVenture.setAgreement(venture.getAgreement());
            updatedVenture.setBrandDetails(venture.getBrandDetails());
            updatedVenture.setContactInfo(venture.getContactInfo());
            return ResponseEntity.ok(ventureRepository.save(updatedVenture));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.badRequest().build();
    }

    public ResponseEntity<Venture> deleteVenture(long id) {
        try {
            Venture venture = ventureRepository.getVentureById(id);
            venture.setStatus(false);
            return ResponseEntity.ok(ventureRepository.save(venture));
        }
        catch(Exception e){
            e.printStackTrace();
        }
        return ResponseEntity.badRequest().build();
    }
}
