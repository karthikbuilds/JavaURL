package com.karthik.JavaURL.url;

import com.karthik.JavaURL.analytics.ClickAnalyticsPublisher;
import com.karthik.JavaURL.analytics.ClickRecordRepository;
import com.karthik.JavaURL.config.AppProperties;
import com.karthik.JavaURL.url.dto.CreateShortUrlRequest;
import com.karthik.JavaURL.url.dto.ShortUrlResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UrlShortenerServiceTest {

    private static final AppProperties PROPERTIES =
            new AppProperties("http://localhost:8080/", 7, 302, null, null);

    @Mock
    private ShortUrlRepository repository;

    @Mock
    private ClickAnalyticsPublisher analyticsPublisher;

    @Mock
    private ClickRecordRepository clickRecordRepository;

    private UrlShortenerService service;

    @BeforeEach
    void setUp() {
        service = new UrlShortenerService(repository, PROPERTIES, analyticsPublisher, clickRecordRepository);
    }

    @Test
    void createUsesCustomAliasWhenAvailable() {
        when(repository.existsByShortCode("my-link")).thenReturn(false);
        when(repository.save(any(ShortUrl.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ShortUrlResponse response = service.create(request("https://example.com/page", "my-link"));

        assertThat(response.shortCode()).isEqualTo("my-link");
        assertThat(response.shortUrl()).isEqualTo("http://localhost:8080/my-link");
        verify(analyticsPublisher, never()).publish(anyString(), anyLong());
    }

    @Test
    void createGeneratesSevenCharCodeWithoutAlias() {
        when(repository.existsByShortCode(anyString())).thenReturn(false);
        when(repository.save(any(ShortUrl.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ShortUrlResponse response = service.create(request("https://example.com/page", null));

        assertThat(response.shortCode()).hasSize(7).matches("[0-9a-zA-Z]{7}");
    }

    @Test
    void createRetriesGenerationOnCollision() {
        when(repository.existsByShortCode(anyString())).thenReturn(true, false);
        when(repository.save(any(ShortUrl.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ShortUrlResponse response = service.create(request("https://example.com/page", null));

        assertThat(response.shortCode()).hasSize(7);
        verify(repository, times(2)).existsByShortCode(anyString());
    }

    @Test
    void createRejectsReservedAliases() {
        assertThatThrownBy(() -> service.create(request("https://example.com", "API")))
                .isInstanceOf(AliasAlreadyInUseException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void createRejectsDuplicateCustomAlias() {
        when(repository.existsByShortCode("taken")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request("https://example.com", "taken")))
                .isInstanceOf(AliasAlreadyInUseException.class);
    }

    @Test
    void createRejectsExpiryInThePast() {
        CreateShortUrlRequest expired = new CreateShortUrlRequest(
                "https://example.com", null, null, Instant.now().minusSeconds(60));

        assertThatThrownBy(() -> service.create(expired))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("future");
    }

    @Test
    void resolveIncrementsClickCountAndPublishesAnalytics() {
        ShortUrl entity = new ShortUrl("abc1234", "https://example.com", null);
        when(repository.findByShortCode("abc1234")).thenReturn(Optional.of(entity));
        when(repository.incrementClickCount("abc1234")).thenReturn(1);

        ShortUrlResponse response = service.resolveAndCount("abc1234");

        assertThat(response.clickCount()).isEqualTo(1);
        verify(repository).incrementClickCount("abc1234");
        verify(analyticsPublisher).publish("abc1234", 1L);
    }

    @Test
    void resolveThrowsNotFoundForUnknownCode() {
        when(repository.findByShortCode("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resolveAndCount("missing"))
                .isInstanceOf(UrlNotFoundException.class);
    }

    @Test
    void resolveMarksExpiredLinksInactiveAndThrowsGone() {
        ShortUrl entity = new ShortUrl("oldlink", "https://example.com",
                Instant.now().minus(1, ChronoUnit.HOURS));
        when(repository.findByShortCode("oldlink")).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> service.resolveAndCount("oldlink"))
                .isInstanceOf(UrlNotAvailableException.class);

        assertThat(entity.isActive()).isFalse();
        verify(analyticsPublisher, never()).publish(anyString(), anyLong());
    }

    @Test
    void deactivateIsIdempotent() {
        ShortUrl inactive = new ShortUrl("deadone", "https://example.com", null);
        inactive.setActive(false);
        when(repository.findByShortCode("deadone")).thenReturn(Optional.of(inactive));

        service.deactivate("deadone");

        verify(repository, never()).save(any());
    }

    private CreateShortUrlRequest request(String longUrl, String alias) {
        return new CreateShortUrlRequest(longUrl, alias, null, null);
    }
}