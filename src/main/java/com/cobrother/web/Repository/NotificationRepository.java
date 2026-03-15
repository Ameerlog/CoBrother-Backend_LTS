package com.cobrother.web.Repository;

import com.cobrother.web.Entity.notification.Notification;
import com.cobrother.web.Entity.user.AppUser;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipientOrderByCreatedAtDesc(AppUser recipient, Pageable pageable);

    List<Notification> findByRecipientOrderByCreatedAtDesc(AppUser recipient);

    long countByRecipientAndIsReadFalse(AppUser recipient);

    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.recipient = :recipient AND n.isRead = false")
    void markAllReadByRecipient(AppUser recipient);
}