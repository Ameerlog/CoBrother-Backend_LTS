package com.cobrother.web.service.likes;

import com.cobrother.web.Entity.likes.Like;
import com.cobrother.web.Entity.likes.LikeType;
import com.cobrother.web.Entity.user.AppUser;
import com.cobrother.web.Repository.LikeRepository;
import com.cobrother.web.service.auth.CurrentUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class LikeService {

    @Autowired private LikeRepository likeRepository;
    @Autowired private CurrentUserService currentUserService;

    // ── Toggle like — returns new state ──────────────────────────────────────
    public ResponseEntity<Map<String, Object>> toggle(String type, Long entityId) {
        AppUser me = currentUserService.getCurrentUser();
        LikeType likeType;
        try {
            likeType = LikeType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid type"));
        }

        Optional<Like> existing = likeRepository.findByUserAndLikeTypeAndEntityId(me, likeType, entityId);
        boolean liked;
        if (existing.isPresent()) {
            likeRepository.delete(existing.get());
            liked = false;
        } else {
            likeRepository.save(new Like(me, likeType, entityId));
            liked = true;
        }

        long count = likeRepository.countByLikeTypeAndEntityId(likeType, entityId);
        return ResponseEntity.ok(Map.of("liked", liked, "count", count));
    }

    // ── Get like status + count for one entity ────────────────────────────────
    public ResponseEntity<Map<String, Object>> getStatus(String type, Long entityId) {
        AppUser me = currentUserService.getCurrentUser();
        LikeType likeType = LikeType.valueOf(type.toUpperCase());
        boolean liked = likeRepository.existsByUserAndLikeTypeAndEntityId(me, likeType, entityId);
        long count = likeRepository.countByLikeTypeAndEntityId(likeType, entityId);
        return ResponseEntity.ok(Map.of("liked", liked, "count", count));
    }

    // ── Bulk status check — for rendering cards efficiently ───────────────────
    // Returns a map of entityId -> {liked, count}
    public ResponseEntity<Map<String, Object>> getBulkStatus(String type, List<Long> entityIds) {
        AppUser me = currentUserService.getCurrentUser();
        LikeType likeType = LikeType.valueOf(type.toUpperCase());

        List<Long> likedIds = likeRepository.findEntityIdsByUserAndLikeType(me, likeType);
        Set<Long> likedSet = new HashSet<>(likedIds);

        Map<String, Object> result = new LinkedHashMap<>();
        for (Long id : entityIds) {
            long count = likeRepository.countByLikeTypeAndEntityId(likeType, id);
            result.put(String.valueOf(id), Map.of(
                    "liked", likedSet.contains(id),
                    "count", count
            ));
        }
        return ResponseEntity.ok(result);
    }

    // ── Who liked (owner only) ────────────────────────────────────────────────
    public ResponseEntity<?> getWhoLiked(String type, Long entityId) {
        LikeType likeType = LikeType.valueOf(type.toUpperCase());
        List<Like> likes = likeRepository.findByLikeTypeAndEntityId(likeType, entityId);
        List<Map<String, Object>> result = likes.stream().map(l -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("userId", l.getUser().getId());
            m.put("name", l.getUser().getFirstname() + " " + l.getUser().getLastname());
            m.put("email", l.getUser().getEmail());
            m.put("likedAt", l.getCreatedAt());
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(Map.of("count", likes.size(), "likers", result));
    }

    // ── Dashboard: get liked entity IDs by type ───────────────────────────────
    public ResponseEntity<List<Long>> getMyLikedIds(String type) {
        AppUser me = currentUserService.getCurrentUser();
        LikeType likeType = LikeType.valueOf(type.toUpperCase());
        return ResponseEntity.ok(likeRepository.findEntityIdsByUserAndLikeType(me, likeType));
    }
}