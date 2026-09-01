package com.karthik.JavaURL.url;

import com.karthik.JavaURL.analytics.ClickRecordStore;
import com.karthik.JavaURL.config.AppProperties;
import com.karthik.JavaURL.url.dto.ShortUrlResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Map;

/**
 * Public-facing endpoints: the root service descriptor and the actual
 * {@code GET /{code}} redirect performed by visitors of a short link.
 */
@RestController
@RequiredArgsConstructor
public class RedirectController {

    private final UrlShortenerService service;
    private final AppProperties properties;
    private final ClickRecordStore clickRecordStore;

    /** Simple service descriptor shown at the root path. */
    @GetMapping("/")
    public Map<String, Object> index() {
        return Map.of(
                "application", "JavaURL",
                "status", "up",
                "api", "/api/v1/urls",
                "websocket", "ws://host/ws (STOMP), topic /topic/clicks/{code}");
    }

    /** Redirects visitors of a short link to the destination URL, counting the click. */
    @GetMapping("/{code:[A-Za-z0-9_-]{3,64}}")
    public ResponseEntity<Void> redirect(@PathVariable String code, HttpServletRequest request) {
        ShortUrlResponse target = service.resolveAndCount(code);
        // Fire-and-forget: captures referrer/agent/origin without slowing the redirect.
        clickRecordStore.record(code, request.getHeader("Referer"),
                request.getHeader("User-Agent"), clientIp(request));
        return ResponseEntity.status(properties.redirectStatus())
                .location(URI.create(target.longUrl()))
                .build();
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}