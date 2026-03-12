package com.cobrother.web.Repository;

import com.cobrother.web.Entity.cobranding.Domain;
import com.cobrother.web.Entity.user.AppUser;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DomainRepository extends JpaRepository<Domain, Long> {
    Domain getDomainById(long id);
    List<Domain> findByListedBy(AppUser user);
    List<Domain> findByPurchasedBy(AppUser user);
    List<Domain> findByStatusTrue(); // all active listings
}
