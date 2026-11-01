package edu.sezg534.ratelimiter.support;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation linking a test method to an official Acceptance Criterion ID (e.g. "AC-01").
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface AcceptanceCriterion {
    String value();
}
