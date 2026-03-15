package com.cobrother.web.controller.likes;

import com.cobrother.web.service.likes.LikeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("api/v1/likes")
public class LikeController {

    @Autowired private LikeService likeService;

    @PostMapping("/{type}/{entityId}/toggle")
    public ResponseEntity<Map<String, Object>> toggle(
            @PathVariable String type,
            @PathVariable Long entityId) {
        return likeService.toggle(type, entityId);
    }

    @GetMapping("/{type}/{entityId}/status")
    public ResponseEntity<Map<String, Object>> getStatus(
            @PathVariable String type,
            @PathVariable Long entityId) {
        return likeService.getStatus(type, entityId);
    }

    @PostMapping("/{type}/bulk-status")
    public ResponseEntity<Map<String, Object>> getBulkStatus(
            @PathVariable String type,
            @RequestBody List<Long> entityIds) {
        return likeService.getBulkStatus(type, entityIds);
    }

    @GetMapping("/{type}/{entityId}/who-liked")
    public ResponseEntity<?> getWhoLiked(
            @PathVariable String type,
            @PathVariable Long entityId) {
        return likeService.getWhoLiked(type, entityId);
    }

    @GetMapping("/{type}/my-liked")
    public ResponseEntity<List<Long>> getMyLiked(@PathVariable String type) {
        return likeService.getMyLikedIds(type);
    }
}