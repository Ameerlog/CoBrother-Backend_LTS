package com.cobrother.web.Repository;

import com.cobrother.web.Entity.likes.Like;
import com.cobrother.web.Entity.likes.LikeType;
import com.cobrother.web.Entity.user.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface LikeRepository extends JpaRepository<Like, Long> {

    Optional<Like> findByUserAndLikeTypeAndEntityId(AppUser user, LikeType likeType, Long entityId);

    long countByLikeTypeAndEntityId(LikeType likeType, Long entityId);

    boolean existsByUserAndLikeTypeAndEntityId(AppUser user, LikeType likeType, Long entityId);

    // For dashboard — get all entity IDs liked by user of a given type
    @Query("SELECT l.entityId FROM Like l WHERE l.user = :user AND l.likeType = :likeType")
    List<Long> findEntityIdsByUserAndLikeType(AppUser user, LikeType likeType);

    // Who liked a specific entity (owner view)
    List<Like> findByLikeTypeAndEntityId(LikeType likeType, Long entityId);
}