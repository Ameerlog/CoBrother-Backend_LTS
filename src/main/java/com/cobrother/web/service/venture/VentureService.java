package com.cobrother.web.service.venture;

import com.cobrother.web.Entity.coventure.Venture;
import com.cobrother.web.Entity.user.AppUser;
import com.cobrother.web.Repository.VentureRepository;
import com.cobrother.web.service.auth.CurrentUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VentureService {

    @Autowired
    private VentureRepository ventureRepository;

    @Autowired
    private CurrentUserService currentUserService;

    public List<Venture> getAllVentures() {
        return ventureRepository.findAll();
    }

    public List<Venture> getVenturesByUser(AppUser user) {
        return ventureRepository.findByListedBy(user);
        // Add to VentureRepository: List<Venture> findByListedBy(AppUser listedBy);
    }

    public Venture getVentureEntity(long id) {
        return ventureRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Venture not found: " + id));
    }

    public Venture addVentureEntity(Venture venture) {
        return ventureRepository.save(venture);
    }



    public Venture updateVentureEntity(long id, Venture incoming) {
        Venture existing = getVentureEntity(id);

        // Preserve the S3 image URL — never overwrite from form PUT
        String existingImageUrl = existing.getBrandDetails() != null
                ? existing.getBrandDetails().getVentureImageUrl()
                : null;

        existing.setBrandDetails(incoming.getBrandDetails());
        existing.setContactInfo(incoming.getContactInfo());
        existing.setAgreement(incoming.getAgreement());
        existing.setStatus(incoming.isStatus());
        existing.setStage(incoming.getStage());
        existing.setLookingFor(incoming.getLookingFor());
        existing.setCurrentProblem(incoming.getCurrentProblem());

        // Restore image URL after overwrite
        if (existing.getBrandDetails() != null && existingImageUrl != null) {
            existing.getBrandDetails().setVentureImageUrl(existingImageUrl);
        }


        return ventureRepository.save(existing);
    }

    public void deleteVenture(long id) {
        ventureRepository.deleteById(id);
    }


//    public ResponseEntity<List<Venture>> getAllVenture() {
//
//        try{
//            AppUser user = currentUserService.getCurrentUser();
//            return ResponseEntity.ok(ventureRepository.findVenturesBy);
//        }
//    }
}
