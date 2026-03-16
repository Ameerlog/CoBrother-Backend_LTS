package com.cobrother.web.Repository;

import com.cobrother.web.Entity.cobranding.DomainEnquiry;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DomainEnquiryRepository extends JpaRepository<DomainEnquiry, Long> {
    List<DomainEnquiry> findAllByOrderByCreatedAtDesc();
    boolean existsByDomainIdAndEnquirerId(Long domainId, Long enquirerId);
}