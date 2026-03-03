package com.cobrother.web.Repository;

import com.cobrother.web.Entity.coventure.Venture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;

@Repository
public interface VentureRepository extends JpaRepository<Venture, Long> {

    @Query("select v from Venture v where v.id=?1 and v.status=true")
    Venture getVentureById(Long id);
}