package com.cobrother.web.controller.notification;

import com.cobrother.web.Entity.notification.Notification;
import com.cobrother.web.service.notification.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("api/v1/notifications")
public class NotificationController {

    @Autowired private NotificationService notificationService;

    @GetMapping("/recent")
    public ResponseEntity<List<Notification>> getRecent() {
        return notificationService.getRecent();
    }

    @GetMapping("/all")
    public ResponseEntity<List<Notification>> getAll() {
        return notificationService.getAll();
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount() {
        return notificationService.getUnreadCount();
    }

    @PutMapping("/mark-all-read")
    public ResponseEntity<?> markAllRead() {
        return notificationService.markAllRead();
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<?> markOneRead(@PathVariable Long id) {
        return notificationService.markOneRead(id);
    }
}