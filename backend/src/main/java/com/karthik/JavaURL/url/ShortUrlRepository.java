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

    /**
     * Atomically increments the click counter for a code and returns the new value.
     * Because the increment happens in a single SQL statement, concurrent redirects
     * cannot lose updates (guards against a read-modify-write race in the service).
     */
    @Modifying
    @Query("update ShortUrl s set s.clickCount = s.clickCount + 1 where s.shortCode = :code and s.active = true")
    int incrementClickCount(@Param("code") String code);

    @Modifying
    @Query("delete from ShortUrl s where s.expiresAt is not null and s.expiresAt < :cutoff")
    int deleteByExpiresAtBefore(@Param("cutoff") Instant cutoff);
}