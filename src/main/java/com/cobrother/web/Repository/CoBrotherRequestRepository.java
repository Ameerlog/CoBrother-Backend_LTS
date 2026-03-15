package com.cobrother.web.Repository;

import com.cobrother.web.Entity.cobrother.CoBrotherRequest;
import com.cobrother.web.Entity.cobrother.CoBrotherRequestStatus;
import com.cobrother.web.Entity.cobrother.RequestType;
import com.cobrother.web.Entity.user.AppUser;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CoBrotherRequestRepository extends JpaRepository<CoBrotherRequest, Long> {
    List<CoBrotherRequest> findByLister(AppUser lister);

    List<CoBrotherRequest> findAllByOrderByCreatedAtDesc();

    List<CoBrotherRequest> findByAssignedCoBrotherOrderByCreatedAtDesc(AppUser coBrother);

    // Add these two methods
    boolean existsByEntityIdAndRequestTypeAndAssignedCoBrother(
            Long entityId, RequestType requestType, AppUser coBrother);

    // Check if lister already has a non-cancelled payment request for this entity
    boolean existsByEntityIdAndRequestTypeAndListerAndStatusNot(
            Long entityId, RequestType requestType, AppUser lister,
            CoBrotherRequestStatus status);

    // Find active (non-cancelled, non-rejected) request for entity
    Optional<CoBrotherRequest> findByEntityIdAndRequestTypeAndStatusNotIn(
            Long entityId, RequestType requestType,
            List<CoBrotherRequestStatus> statuses);

    List<CoBrotherRequest> findByEntityIdAndRequestType(Long entityId, RequestType requestType);
}
