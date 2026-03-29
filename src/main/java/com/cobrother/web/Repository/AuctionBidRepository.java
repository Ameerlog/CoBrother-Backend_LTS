

// AuctionBidRepository.java
package com.cobrother.web.Repository;

import com.cobrother.web.Entity.cobranding.AuctionBid;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AuctionBidRepository extends JpaRepository<AuctionBid, Long> {
    List<AuctionBid> findByAuctionIdOrderByBidTimeDesc(Long auctionId);
    List<AuctionBid> findByAuctionIdOrderByAmountDesc(Long auctionId);
    long countByAuctionId(Long auctionId);
}
