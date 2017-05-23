package org.zalando.nakadiproducer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Qualifier annotation for a FlywayCallback to be injected in to nakadi-producers Flyway instance.
 */
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE,
    ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Qualifier
public @interface NakadiProducerFlywayCallback {
}
