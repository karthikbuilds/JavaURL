package com.karthik.JavaURL.url;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end API tests running against the full Spring context with an
 * in-memory H2 database (see src/test/resources/application.properties).
 */
@SpringBootTest
@AutoConfigureMockMvc
class UrlApiIntegrationTest {

    private static final Pattern SHORT_CODE_FIELD = Pattern.compile("\"shortCode\":\"([^\"]+)\"");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ShortUrlRepository repository;

    @AfterEach
    void cleanUp() {
        repository.deleteAll();
    }

    private MvcResult createShortUrl(String body) throws Exception {
        return mockMvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shortUrl").value(startsWith("http://localhost:8080/")))
                .andExpect(jsonPath("$.clickCount").value(0))
                .andReturn();
    }

    private String extractCode(MvcResult result) throws UnsupportedEncodingException {
        Matcher matcher = SHORT_CODE_FIELD.matcher(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(matcher.find()).as("response contains shortCode").isTrue();
        return matcher.group(1);
    }

    @Test
    void createRedirectAndStatsFullFlow() throws Exception {
        String longUrl = "https://example.com/articles/how-to-build-a-url-shortener";

        String code = extractCode(createShortUrl("{\"longUrl\":\"" + longUrl + "\"}"));
        assertThat(code).hasSize(7);

        mockMvc.perform(get("/" + code))
                .andExpect(status().isFound())
                .andExpect(header().string(HttpHeaders.LOCATION, longUrl));

        mockMvc.perform(get("/api/v1/urls/" + code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.longUrl").value(longUrl))
                .andExpect(jsonPath("$.clickCount").value(1));
    }

    @Test
    void customAliasIsHonouredAndDuplicatesAreRejected() throws Exception {
        createShortUrl("{\"longUrl\":\"https://example.com/alias-target\",\"customAlias\":\"karthiks-link\"}");

        mockMvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"longUrl\":\"https://example.com/other\",\"customAlias\":\"karthiks-link\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Custom alias 'karthiks-link' is already in use"));

        mockMvc.perform(get("/karthiks-link"))
                .andExpect(status().isFound())
                .andExpect(header().string(HttpHeaders.LOCATION, "https://example.com/alias-target"));
    }

    @Test
    void deletedLinkStopsRedirectingWithGone() throws Exception {
        createShortUrl("{\"longUrl\":\"https://example.com/doomed\",\"customAlias\":\"doomed-link\"}");

        mockMvc.perform(delete("/api/v1/urls/doomed-link"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/doomed-link"))
                .andExpect(status().isGone());

        // Deleting again stays idempotent (204).
        mockMvc.perform(delete("/api/v1/urls/doomed-link"))
                .andExpect(status().isNoContent());
    }

    @Test
    void unknownCodeReturns404() throws Exception {
        mockMvc.perform(get("/zzzzzzz"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void rejectsInvalidLongUrlsWithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"longUrl\":\"not a url at all\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.longUrl").exists());
    }

    @Test
    void listEndpointReturnsCreatedLinks() throws Exception {
        createShortUrl("{\"longUrl\":\"https://example.com/first\",\"customAlias\":\"first-one\"}");
        createShortUrl("{\"longUrl\":\"https://example.com/second\",\"customAlias\":\"second-1\"}");

        mockMvc.perform(get("/api/v1/urls"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));
    }
}