package com.karthik.JavaURL.url;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Set;

/**
 * Rejects anything that is not an absolute http(s) URL pointing at a host.
 */
public class ValidUrlValidator implements ConstraintValidator<ValidUrl, String> {

    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return false;
        }
        try {
            URI uri = new URI(value.trim());
            return uri.isAbsolute()
                    && uri.getHost() != null
                    && ALLOWED_SCHEMES.contains(uri.getScheme().toLowerCase(Locale.ROOT));
        } catch (URISyntaxException ex) {
            return false;
        }
    }
}