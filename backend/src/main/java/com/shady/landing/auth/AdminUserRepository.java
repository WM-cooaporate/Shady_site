package com.shady.landing.auth;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdminUserRepository extends JpaRepository<AdminUser, Long> {

    /** Matches the {@code lower(email)} unique index. */
    @Query("select a from AdminUser a where lower(a.email) = lower(:email)")
    Optional<AdminUser> findByEmail(@Param("email") String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from AdminUser a where a.id = :id")
    Optional<AdminUser> findByIdForUpdate(@Param("id") Long id);

    @Query("select a.passwordChangedAt from AdminUser a where a.id = :id")
    Optional<Instant> findPasswordChangedAt(@Param("id") Long id);
}
