package com.cobrother.web.Repository;

import com.cobrother.web.Entity.user.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface UserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByEmail(String email);

    Optional<AppUser> findByVerificationToken(String verificationToken);

    boolean existsByEmail(String email);
}
