package com.karthik.JavaURL.url;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates that a string is an absolute http(s) URL with a host.
 */
@Documented
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE,
        ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidUrlValidator.class)
public @interface ValidUrl {

    String message() default "must be a valid absolute http(s) URL";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}