package com.cobrother.web.service.auction;

import com.cobrother.web.Entity.cobranding.*;
import com.cobrother.web.Entity.user.AppUser;
import com.cobrother.web.Repository.*;
import com.cobrother.web.model.auction.AuctionUpdateMessage;
import com.cobrother.web.model.auction.BidQueueItem;
import com.cobrother.web.service.auth.MailService;
import com.cobrother.web.service.notification.NotificationService;
import com.cobrother.web.Entity.notification.NotificationType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class AuctionService {

    private static final String BID_QUEUE_PREFIX = "auction:bid_queue:";

    @Autowired private AuctionRepository auctionRepository;
    @Autowired private AuctionBidRepository bidRepository;
    @Autowired private DomainRepository domainRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private RedisTemplate<String, Object> redisTemplate;
    @Autowired private SimpMessagingTemplate messagingTemplate;
    @Autowired private MailService mailService;
    @Autowired private NotificationService notificationService;

    // ── Create auction listing (domain in DRAFT until verified) ──────────────
    public ResponseEntity<?> createAuction(Long domainId, double minBidPrice,
                                           AuctionDuration duration, AppUser lister) {
        Domain domain = domainRepository.getDomainById(domainId);
        if (domain == null) return ResponseEntity.notFound().build();
        if (!domain.getListedBy().getId().equals(lister.getId()))
            return ResponseEntity.status(403).body("Not your domain");
        if (auctionRepository.findByDomainId(domainId).isPresent())
            return ResponseEntity.badRequest().body("Auction already exists for this domain");

        Auction auction = new Auction();
        auction.setDomain(domain);
        auction.setMinBidPrice(minBidPrice);
        auction.setDuration(duration);
        auction.setStatus(AuctionStatus.DRAFT);
        // Start/end time set when verification completes and auction goes ACTIVE

        Auction saved = auctionRepository.save(auction);
        return ResponseEntity.ok(saved);
    }

    // ── Called by DomainVerificationService after successful verification ─────
    @Transactional
    public void activateAuction(Long domainId) {
        auctionRepository.findByDomainId(domainId).ifPresent(auction -> {
            if (auction.getStatus() != AuctionStatus.DRAFT) return;

            LocalDateTime now   = LocalDateTime.now();
            LocalDateTime end   = now.plusDays(auction.getDuration().getDays());

            auction.setStatus(AuctionStatus.ACTIVE);
            auction.setStartTime(now);
            auction.setEndTime(end);
            auction.setOriginalEndTime(end);
            auctionRepository.save(auction);

            // Broadcast auction started
            AuctionUpdateMessage msg = buildMessage(auction, "AUCTION_STARTED");
            msg.setMessage("Auction is now live!");
            broadcast(auction.getId(), msg);
        });
    }

    // ── Place a bid — queued through Redis ───────────────────────────────────
    public ResponseEntity<?> placeBid(Long auctionId, double amount, AppUser bidder) {
        Auction auction = auctionRepository.findById(auctionId).orElse(null);
        if (auction == null) return ResponseEntity.notFound().build();

        if (auction.getStatus() != AuctionStatus.ACTIVE &&
                auction.getStatus() != AuctionStatus.EXTENDED)
            return ResponseEntity.badRequest().body(Map.of("error", "Auction is not active"));

        if (LocalDateTime.now().isAfter(auction.getEndTime()))
            return ResponseEntity.badRequest().body(Map.of("error", "Auction has ended"));

        // Lister cannot bid
        if (auction.getDomain().getListedBy().getId().equals(bidder.getId()))
            return ResponseEntity.badRequest().body(Map.of("error", "You cannot bid on your own domain"));

        // Validate minimum bid
        double minRequired = auction.getCurrentHighestBid() > 0
                ? auction.getCurrentHighestBid() * 1.05   // 5% above current
                : auction.getMinBidPrice();

        if (amount < minRequired) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Bid must be at least ₹" + String.format("%.2f", minRequired),
                    "minRequired", minRequired
            ));
        }

        // Queue bid in Redis — atomic, no race conditions
        String bidderName = bidder.getFirstname() + " " + bidder.getLastname();
        BidQueueItem item = new BidQueueItem(auctionId, bidder.getId(), bidderName, amount);
        redisTemplate.opsForList().leftPush(BID_QUEUE_PREFIX + auctionId, item);

        return ResponseEntity.ok(Map.of(
                "queued", true,
                "message", "Bid placed successfully",
                "amount", amount
        ));
    }

    // ── Process bid queue every 100ms ─────────────────────────────────────────
    @Scheduled(fixedDelay = 100)
    @Transactional
    public void processBidQueue() {
        // Get all active auctions that have queued bids
        List<Auction> activeAuctions = auctionRepository.findByStatus(AuctionStatus.ACTIVE);
        activeAuctions.addAll(auctionRepository.findByStatus(AuctionStatus.EXTENDED));

        for (Auction auction : activeAuctions) {
            String queueKey = BID_QUEUE_PREFIX + auction.getId();
            Object rawItem;

            // Process up to 10 bids per auction per cycle
            int processed = 0;
            while (processed < 10 &&
                    (rawItem = redisTemplate.opsForList().rightPop(queueKey)) != null) {

                BidQueueItem item = convertToBidQueueItem(rawItem);
                if (item == null) { processed++; continue; }

                // Re-validate against latest DB state
                Auction fresh = auctionRepository.findById(auction.getId()).orElse(null);
                if (fresh == null) break;

                double minRequired = fresh.getCurrentHighestBid() > 0
                        ? fresh.getCurrentHighestBid() * 1.05
                        : fresh.getMinBidPrice();

                if (item.getAmount() < minRequired) {
                    // Bid is now below minimum (outbid by concurrent bidder) — silently drop
                    processed++;
                    continue;
                }

                // Save bid to DB
                AppUser bidder = userRepository.findById(item.getBidderId()).orElse(null);
                if (bidder == null) { processed++; continue; }

                AuctionBid bid = new AuctionBid();
                bid.setAuction(fresh);
                bid.setBidder(bidder);
                bid.setAmount(item.getAmount());
                bid.setBidderName(item.getBidderName());
                bidRepository.save(bid);

                // Update auction
                fresh.setCurrentHighestBid(item.getAmount());
                fresh.setCurrentWinner(bidder);
                fresh.setTotalBids(fresh.getTotalBids() + 1);

                // Anti-snipe: extend by 5 mins if bid placed in last 5 mins
                LocalDateTime now = LocalDateTime.now();
                long minutesLeft = ChronoUnit.MINUTES.between(now, fresh.getEndTime());
                if (minutesLeft < 5) {
                    fresh.setEndTime(now.plusMinutes(5));
                    fresh.setStatus(AuctionStatus.EXTENDED);
                }

                auctionRepository.save(fresh);

                // Broadcast update
                AuctionUpdateMessage msg = buildMessage(fresh, "BID_PLACED");
                msg.setLatestBid(new AuctionUpdateMessage.BidSummary(
                        item.getBidderName(), item.getAmount(), LocalDateTime.now()));
                if (fresh.getStatus() == AuctionStatus.EXTENDED) {
                    msg.setMessage("Auction extended! A bid was placed in the final 5 minutes.");
                }
                broadcast(fresh.getId(), msg);

                // Notify previous highest bidder they've been outbid
                notifyOutbid(fresh, bidder);

                processed++;
            }
        }
    }

    // ── End expired auctions every 30 seconds ─────────────────────────────────
    @Scheduled(fixedDelay = 30000)
    @Transactional
    public void endExpiredAuctions() {
        List<Auction> expired = auctionRepository.findExpiredAuctions(LocalDateTime.now());
        for (Auction auction : expired) {
            endAuction(auction);
        }
    }

    @Transactional
    public void endAuction(Auction auction) {
        if (auction.getTotalBids() == 0) {
            auction.setStatus(AuctionStatus.UNSOLD);
        } else {
            auction.setStatus(AuctionStatus.ENDED);
            // Mark winning bid
            bidRepository.findByAuctionIdOrderByAmountDesc(auction.getId())
                    .stream().findFirst().ifPresent(bid -> {
                        bid.setWinningBid(true);
                        bidRepository.save(bid);
                    });
        }
        auctionRepository.save(auction);

        // Broadcast auction ended
        AuctionUpdateMessage msg = buildMessage(auction,
                auction.getStatus() == AuctionStatus.UNSOLD ? "AUCTION_UNSOLD" : "AUCTION_ENDED");
        msg.setMessage(auction.getStatus() == AuctionStatus.UNSOLD
                ? "Auction ended with no bids."
                : auction.getCurrentWinner().getFirstname() + " won with ₹" +
                String.format("%.2f", auction.getCurrentHighestBid()));
        broadcast(auction.getId(), msg);

        // Send emails
        if (auction.getStatus() == AuctionStatus.ENDED) {
            mailService.sendAuctionWinnerEmail(
                    auction.getCurrentWinner().getEmail(),
                    auction.getCurrentWinner().getFirstname(),
                    auction.getDomain().getDomainName() + auction.getDomain().getDomainExtension(),
                    auction.getCurrentHighestBid()
            );
            mailService.sendAuctionEndedSellerEmail(
                    auction.getDomain().getListedBy().getEmail(),
                    auction.getDomain().getListedBy().getFirstname(),
                    auction.getDomain().getDomainName() + auction.getDomain().getDomainExtension(),
                    auction.getCurrentWinner().getFirstname(),
                    auction.getCurrentHighestBid()
            );
            // Notify winner in-app
            notificationService.notify(
                    auction.getCurrentWinner(),
                    NotificationType.DOMAIN_SOLD,
                    "You Won the Auction! 🎉",
                    "You won the auction for " + auction.getDomain().getDomainName() +
                            auction.getDomain().getDomainExtension() +
                            " with a bid of ₹" + String.format("%.2f", auction.getCurrentHighestBid()),
                    "/domains/dashboard"
            );
        }
        // Notify lister
        notificationService.notify(
                auction.getDomain().getListedBy(),
                NotificationType.DOMAIN_SOLD,
                auction.getStatus() == AuctionStatus.UNSOLD ? "Auction Ended — No Bids" : "Domain Auction Won",
                auction.getStatus() == AuctionStatus.UNSOLD
                        ? "Your auction for " + auction.getDomain().getDomainName() +
                        auction.getDomain().getDomainExtension() + " ended with no bids."
                        : "Your auction was won by " + auction.getCurrentWinner().getFirstname() +
                        " for ₹" + String.format("%.2f", auction.getCurrentHighestBid()),
                "/domains/dashboard"
        );
    }

    // ── Re-auction after UNSOLD ───────────────────────────────────────────────
    public ResponseEntity<?> reAuction(Long auctionId, double newMinBid,
                                       AuctionDuration newDuration, AppUser lister) {
        Auction auction = auctionRepository.findById(auctionId).orElse(null);
        if (auction == null) return ResponseEntity.notFound().build();
        if (!auction.getDomain().getListedBy().getId().equals(lister.getId()))
            return ResponseEntity.status(403).body("Not your auction");
        if (auction.getStatus() != AuctionStatus.UNSOLD)
            return ResponseEntity.badRequest().body("Can only re-auction an unsold auction");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime end = now.plusDays(newDuration.getDays());

        auction.setMinBidPrice(newMinBid);
        auction.setDuration(newDuration);
        auction.setCurrentHighestBid(0);
        auction.setCurrentWinner(null);
        auction.setTotalBids(0);
        auction.setStatus(AuctionStatus.ACTIVE);
        auction.setStartTime(now);
        auction.setEndTime(end);
        auction.setOriginalEndTime(end);
        auctionRepository.save(auction);

        AuctionUpdateMessage msg = buildMessage(auction, "AUCTION_STARTED");
        msg.setMessage("Auction re-opened!");
        broadcast(auction.getId(), msg);

        return ResponseEntity.ok(auction);
    }

    // ── Close after UNSOLD (lister takes down) ────────────────────────────────
    public ResponseEntity<?> closeAuction(Long auctionId, AppUser lister) {
        Auction auction = auctionRepository.findById(auctionId).orElse(null);
        if (auction == null) return ResponseEntity.notFound().build();
        if (!auction.getDomain().getListedBy().getId().equals(lister.getId()))
            return ResponseEntity.status(403).body("Not your auction");
        if (auction.getStatus() != AuctionStatus.UNSOLD)
            return ResponseEntity.badRequest().body("Can only close an unsold auction");

        auction.setStatus(AuctionStatus.CLOSED);
        auction.getDomain().setStatus(false);
        auctionRepository.save(auction);
        domainRepository.save(auction.getDomain());

        return ResponseEntity.ok(Map.of("success", true));
    }

    // ── Get auction details + bid history ─────────────────────────────────────
    public ResponseEntity<?> getAuction(Long auctionId) {
        return auctionRepository.findById(auctionId)
                .<ResponseEntity<?>>map(a -> {
                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("auction", a);
                    result.put("bids", bidRepository.findByAuctionIdOrderByBidTimeDesc(auctionId));
                    result.put("totalBids", a.getTotalBids());
                    result.put("currentHighestBid", a.getCurrentHighestBid());
                    result.put("minNextBid", a.getCurrentHighestBid() > 0
                            ? a.getCurrentHighestBid() * 1.05
                            : a.getMinBidPrice());
                    return ResponseEntity.ok(result);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    public ResponseEntity<?> getAuctionByDomain(Long domainId) {
        return auctionRepository.findByDomainId(domainId)
                .<ResponseEntity<?>>map(a -> {
                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("auction", a);
                    result.put("bids", bidRepository.findByAuctionIdOrderByBidTimeDesc(a.getId()));
                    result.put("minNextBid", a.getCurrentHighestBid() > 0
                            ? a.getCurrentHighestBid() * 1.05
                            : a.getMinBidPrice());
                    return ResponseEntity.ok(result);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ── Admin: get all auctions with full bid details ─────────────────────────
    public ResponseEntity<?> getAllAuctionsForAdmin() {
        List<Auction> auctions = auctionRepository.findAll();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Auction a : auctions) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("auction", a);
            item.put("bids", bidRepository.findByAuctionIdOrderByBidTimeDesc(a.getId()));
            item.put("domain", a.getDomain());
            result.add(item);
        }
        return ResponseEntity.ok(result);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private void broadcast(Long auctionId, AuctionUpdateMessage msg) {
        messagingTemplate.convertAndSend("/topic/auction/" + auctionId, msg);
    }

    private AuctionUpdateMessage buildMessage(Auction a, String type) {
        AuctionUpdateMessage msg = new AuctionUpdateMessage();
        msg.setAuctionId(a.getId());
        msg.setType(type);
        msg.setCurrentHighestBid(a.getCurrentHighestBid());
        msg.setTotalBids(a.getTotalBids());
        msg.setEndTime(a.getEndTime());
        msg.setStatus(a.getStatus().name());
        if (a.getCurrentWinner() != null)
            msg.setCurrentWinnerName(a.getCurrentWinner().getFirstname() + " " +
                    a.getCurrentWinner().getLastname().charAt(0) + ".");
        return msg;
    }

    private void notifyOutbid(Auction auction, AppUser newBidder) {
        // Find previous highest bidder (second highest amount)
        List<AuctionBid> bids = bidRepository.findByAuctionIdOrderByAmountDesc(auction.getId());
        if (bids.size() > 1) {
            AppUser outbid = bids.get(1).getBidder();
            if (!outbid.getId().equals(newBidder.getId())) {
                notificationService.notify(
                        outbid,
                        NotificationType.COVENTURE_APPLICATION_STATUS_CHANGED,
                        "You've Been Outbid",
                        "Someone outbid you on " + auction.getDomain().getDomainName() +
                                auction.getDomain().getDomainExtension() +
                                ". Current highest: ₹" + String.format("%.2f", auction.getCurrentHighestBid()),
                        "/domains"
                );
            }
        }
    }

    private BidQueueItem convertToBidQueueItem(Object raw) {
        try {
            if (raw instanceof BidQueueItem) return (BidQueueItem) raw;
            if (raw instanceof java.util.LinkedHashMap) {
                @SuppressWarnings("unchecked")
                java.util.LinkedHashMap<String, Object> map = (java.util.LinkedHashMap<String, Object>) raw;
                BidQueueItem item = new BidQueueItem();
                item.setAuctionId(((Number) map.get("auctionId")).longValue());
                item.setBidderId(((Number) map.get("bidderId")).longValue());
                item.setBidderName((String) map.get("bidderName"));
                item.setAmount(((Number) map.get("amount")).doubleValue());
                return item;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public ResponseEntity<?> getActiveAuctions() {
        List<Auction> active = new ArrayList<>(
                auctionRepository.findByStatus(AuctionStatus.ACTIVE));
        active.addAll(auctionRepository.findByStatus(AuctionStatus.EXTENDED));
        // Sort by endTime ascending — soonest ending first
        active.sort(Comparator.comparing(Auction::getEndTime));
        return ResponseEntity.ok(active);
    }
}