package com.cobrother.web.Repository;

import com.cobrother.web.Entity.cobranding.Domain;
import com.cobrother.web.Entity.user.AppUser;
import com.cobrother.web.Entity.user.UserRole;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DomainRepository extends JpaRepository<Domain, Long> {


    Domain getDomainById(long id);
    List<Domain> findByListedBy(AppUser user);
    List<Domain> findByPurchasedBy(AppUser user);
    List<Domain> findByStatusTrue(); // all active listings// Add to DomainRepository
    List<Domain> findByDomainNameAndDomainExtensionAndStatusTrue(String domainName, String domainExtension);
    List<Domain> findByDomainNameAndDomainExtension(String domainName, String domainExtension);

    @Query("select d from Domain d where d.listedBy.role = ?1")
    List<Domain> findAllDomainsByListedByCobrother(UserRole role);


}
