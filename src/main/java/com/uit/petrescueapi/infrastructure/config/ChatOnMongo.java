package com.uit.petrescueapi.infrastructure.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Bean is active when {@code app.storage.chat=mongo}. See migrate.md §7.1. */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@ConditionalOnProperty(name = "app.storage.chat", havingValue = "mongo", matchIfMissing = false)
public @interface ChatOnMongo {
}
