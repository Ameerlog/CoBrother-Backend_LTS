package com.cobrother.web.Repository;

import com.cobrother.web.Entity.coventure.CoVenture;
import com.cobrother.web.Entity.user.AppUser;
import com.cobrother.web.Entity.coventure.Venture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CoVentureRepository extends JpaRepository<CoVenture, Long> {

    boolean existsByVentureAndApplicant(Venture venture, AppUser applicant);
}