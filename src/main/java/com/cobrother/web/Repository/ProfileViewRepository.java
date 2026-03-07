package com.cobrother.web.Repository;

import com.cobrother.web.Entity.analytics.ProfileView;
import com.cobrother.web.Entity.community.Community;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ProfileViewRepository extends JpaRepository<ProfileView, Long> {

    List<ProfileView> findByProfileId(Long profileId);

    @Query("SELECT v FROM ProfileView v WHERE v.profile.id = :profileId AND v.viewedAt >= :since")
    List<ProfileView> findByProfileIdSince(Long profileId, LocalDateTime since);

    long countByProfileId(Long profileId);
    List<ProfileView> findByProfile(Community profile);
    List<ProfileView> findByProfileAndViewedAtAfter(Community profile, LocalDateTime since);
    long countByProfile(Community profile);
    List<ProfileView> findByProfile_Id(Long profileId);
}