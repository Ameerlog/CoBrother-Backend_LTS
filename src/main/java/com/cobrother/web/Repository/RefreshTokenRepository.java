package com.cobrother.web.Repository;

import com.cobrother.web.Entity.AppUser;
import com.cobrother.web.Entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    Optional<RefreshToken> findByUserAndRevokedFalse(AppUser user);

    void deleteByUser(AppUser user);
}