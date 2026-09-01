package com.karthik.JavaURL.url;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface ShortUrlRepository extends JpaRepository<ShortUrl, Long> {

    Optional<ShortUrl> findByShortCode(String shortCode);

    boolean existsByShortCode(String shortCode);

    Page<ShortUrl> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Modifying
    @Query("update ShortUrl s set s.active = false where s.active = true and s.expiresAt is not null and s.expiresAt < :now")
    int deactivateExpired(@Param("now") Instant now);

    @Modifying
    @Query("delete from ShortUrl s where s.expiresAt is not null and s.expiresAt < :cutoff")
    int deleteByExpiresAtBefore(@Param("cutoff") Instant cutoff);
}