// AuctionRepository.java
package com.cobrother.web.Repository;

import com.cobrother.web.Entity.cobranding.Auction;
import com.cobrother.web.Entity.cobranding.AuctionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AuctionRepository extends JpaRepository<Auction, Long> {
    Optional<Auction> findByDomainId(Long domainId);
    List<Auction> findByStatus(AuctionStatus status);
    @Query("SELECT a FROM Auction a WHERE a.status IN ('ACTIVE','EXTENDED') AND a.endTime <= :now")
    List<Auction> findExpiredAuctions(LocalDateTime now);
}
