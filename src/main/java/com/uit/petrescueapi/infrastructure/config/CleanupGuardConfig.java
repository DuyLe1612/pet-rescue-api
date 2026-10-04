package com.uit.petrescueapi.infrastructure.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

/**
 * Defensive checks that fail fast (with a clear log line) if a previously
 * removed dependency or method comes back to the codebase. Runs once at
 * startup after Spring finishes property + bean injection.
 *
 * <p>Pairs with V22 (Flyway drops the {@code BATCH_*} tables).</p>
 */
@Configuration
@Slf4j
public class CleanupGuardConfig {

    @PostConstruct
    void assertNoDeadBatchStarter() {
        // If a future change re-adds spring-boot-starter-batch to build.gradle.kts,
        // this guard emits a loud warning at startup.
        try {
            Class.forName("org.springframework.batch.core.Job");
            log.warn("Cleanup guard: Spring Batch is on the classpath. "
                    + "Did you re-add 'spring-boot-starter-batch'? "
                    + "Make sure to also revert V22 migration and the cleanup of "
                    + "the dead 'findAllByIds' / 'assignPermissions' / 'getPermissions' methods.");
        } catch (ClassNotFoundException ignored) {
            log.info("Cleanup guard: Spring Batch confirmed removed from classpath.");
        }
    }
}