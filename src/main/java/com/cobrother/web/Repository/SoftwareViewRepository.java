package com.cobrother.web.Repository;

import com.cobrother.web.Entity.cocreation.SoftwareView;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SoftwareViewRepository extends JpaRepository<SoftwareView, Long> {
    List<SoftwareView> findBySoftware_Id(Long softwareId);
}