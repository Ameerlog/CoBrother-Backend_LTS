package com.cobrother.web.Repository;

import com.cobrother.web.Entity.cocreation.SoftwarePurchase;
import com.cobrother.web.Entity.cocreation.SoftwarePaymentStatus;
import com.cobrother.web.Entity.user.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SoftwarePurchaseRepository extends JpaRepository<SoftwarePurchase, Long> {
    List<SoftwarePurchase> findByBuyer(AppUser buyer);
    List<SoftwarePurchase> findBySoftware_Id(Long softwareId);
    Optional<SoftwarePurchase> findBySoftware_IdAndBuyer(Long softwareId, AppUser buyer);
    long countBySoftware_IdAndPaymentStatus(Long softwareId, SoftwarePaymentStatus status);
    Optional<SoftwarePurchase> findByRazorpayOrderId(String orderId);

    List<SoftwarePurchase> findByPaymentStatus(SoftwarePaymentStatus softwarePaymentStatus);

    List<SoftwarePurchase> findByPaymentStatusOrderByCreatedAtDesc(SoftwarePaymentStatus softwarePaymentStatus);
}