package com.karthik.JavaURL.analytics;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * A single redirect attempt, written asynchronously so recording analytics never
 * slows down a redirect. Enables per-click detail (referrer, user agent, origin).
 */
@Entity
@Table(name = "click_records", indexes = {
        @Index(name = "idx_click_records_short_code", columnList = "short_code")
})
@Getter
@NoArgsConstructor
public class ClickRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "short_code", nullable = false, length = 64)
    private String shortCode;

    @Column(name = "clicked_at", nullable = false)
    private Instant clickedAt;

    @Column(name = "referer", length = 2048)
    private String referer;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "ip", length = 64)
    private String ip;

    public ClickRecord(String shortCode, Instant clickedAt, String referer, String userAgent, String ip) {
        this.shortCode = shortCode;
        this.clickedAt = clickedAt;
        this.referer = referer;
        this.userAgent = userAgent;
        this.ip = ip;
    }
}