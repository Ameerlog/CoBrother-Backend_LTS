package com.cobrother.web.Repository;

import com.cobrother.web.Entity.analytics.VentureView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface VentureViewRepository extends JpaRepository<VentureView, Long> {

    List<VentureView> findByVentureId(Long ventureId);

    @Query("SELECT v FROM VentureView v WHERE v.venture.id = :ventureId AND v.viewedAt >= :since")
    List<VentureView> findByVentureIdSince(Long ventureId, LocalDateTime since);

    long countByVentureId(Long ventureId);

    List<VentureView> findByVenture_Id(Long ventureId);
}