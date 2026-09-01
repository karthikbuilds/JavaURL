package com.karthik.JavaURL.url;

import com.karthik.JavaURL.url.dto.CreateShortUrlRequest;
import com.karthik.JavaURL.url.dto.ShortUrlResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import java.net.URI;

@RestController
@RequestMapping("/api/v1/urls")
@RequiredArgsConstructor
public class ApiController {

    private final UrlShortenerService service;

    /**
     * Creates a short URL.
     *
     * <pre>
     * POST /api/v1/urls
     * {"longUrl": "https://example.com/a/very/long/path", "customAlias": "my-link", "expiresInDays": 7}
     * </pre>
     */
    @PostMapping
    public ResponseEntity<ShortUrlResponse> create(@Valid @RequestBody CreateShortUrlRequest request) {
        ShortUrlResponse response = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).location(URI.create(response.shortUrl())).body(response);
    }

    /** Lists all short URLs, newest first. Supports ?page=&amp;size=&amp;sort=. */
    @GetMapping
    public PagedModel<ShortUrlResponse> list(@PageableDefault(size = 20, sort = "createdAt",
            direction = Sort.Direction.DESC) Pageable pageable) {
        return service.list(pageable);
    }

    /** Returns metadata and click statistics for one short URL. */
    @GetMapping("/{code:[A-Za-z0-9_-]{1,64}}")
    public ShortUrlResponse stats(@PathVariable String code) {
        return service.stats(code);
    }

    /** Deactivates a short URL so that it stops redirecting. Idempotent. */
    @DeleteMapping("/{code:[A-Za-z0-9_-]{1,64}}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String code) {
        service.deactivate(code);
    }
}