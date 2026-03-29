package com.cobrother.web.Repository;

import com.cobrother.web.Entity.cobranding.Domain;
import com.cobrother.web.Entity.cocreation.Software;
import com.cobrother.web.Entity.user.AppUser;
import com.cobrother.web.Entity.user.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SoftwareRepository extends JpaRepository<Software, Long> {
    List<Software> findByStatusTrue();
    List<Software> findByListedBy(AppUser user);
    Software findSoftwareById(Long id);


    List<Software> findAll();
}