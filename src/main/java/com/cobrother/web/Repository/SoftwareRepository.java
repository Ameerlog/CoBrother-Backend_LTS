package com.cobrother.web.Repository;

import com.cobrother.web.Entity.cocreation.Software;
import com.cobrother.web.Entity.user.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SoftwareRepository extends JpaRepository<Software, Long> {
    List<Software> findByStatusTrue();
    List<Software> findByListedBy(AppUser user);
    List<Software> findByPurchasedBy(AppUser user);
    Software findSoftwareById(Long id);
}