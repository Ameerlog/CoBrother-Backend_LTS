package com.cobrother.web.Repository;

import com.cobrother.web.Entity.cobranding.Domain;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DomainRepository extends JpaRepository<Domain, Long> {
    Domain getDomainById(long id);
}
