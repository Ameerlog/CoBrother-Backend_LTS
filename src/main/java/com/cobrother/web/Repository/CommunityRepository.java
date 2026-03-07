package com.cobrother.web.Repository;

import com.cobrother.web.Entity.community.Community;
import com.cobrother.web.Entity.user.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CommunityRepository extends JpaRepository<Community, Long> {
    Optional<Community> findByAppUserId(Long userId);
    Optional<Community> findByLinkedInId(String linkedInId);
    boolean existsByAppUserId(Long userId);

    Optional<Community> findByAppUser(AppUser viewer);
}