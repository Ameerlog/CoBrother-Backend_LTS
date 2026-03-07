package com.cobrother.web.Repository;

import com.cobrother.web.Entity.coventure.CoVenture;
import com.cobrother.web.Entity.coventure.CoVentureStatus;
import com.cobrother.web.Entity.coventure.Venture;
import com.cobrother.web.Entity.user.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CoVentureRepository extends JpaRepository<CoVenture, Long> {

    /**
     * Used by GET /my-status — returns the application if it exists.
     */
    Optional<CoVenture> findByVentureIdAndApplicantId(Long ventureId, Long applicantId);

    /**
     * Used by POST apply — fast existence check before inserting.
     */
    boolean existsByVentureIdAndApplicantId(Long ventureId, Long applicantId);

    boolean existsByVentureAndApplicant(Venture venture, AppUser currentUser);


    List<CoVenture> findByApplicantId(Long applicantId);
    List<CoVenture> findByVentureListedById(Long listedById);
    List<CoVenture> findByVentureListedByIdAndStatus(Long listedById, CoVentureStatus status);
}