package com.uit.petrescueapi.infrastructure.persistence.mongo;

import com.mongodb.MongoException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Retries a domain-service call whose MongoDB transaction was aborted with a
 * {@code TransientTransactionError} (e.g. WriteConflict when two users send to the same
 * conversation at once). PostgreSQL would block on the row lock instead; MongoDB aborts
 * immediately, so without this one of the two sends would fail.
 *
 * <p>Ordered first so it wraps <b>outside</b> the {@code @Transactional} proxy — every attempt
 * runs in a fresh transaction. Errors that are not transient are rethrown unchanged.</p>
 */
@Slf4j
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MongoTransientRetryAspect {

    static final int MAX_ATTEMPTS = 10;
    private static final long MAX_BACKOFF_MS = 500;

    @Around("execution(public * com.uit.petrescueapi.domain.service.ChatDomainService.*(..))"
            + " || execution(public * com.uit.petrescueapi.domain.service.PostDomainService.*(..))"
            + " || execution(public * com.uit.petrescueapi.domain.service.CommentDomainService.*(..))"
            + " || execution(public * com.uit.petrescueapi.domain.service.LikeDomainService.*(..))")
    public Object retryTransient(ProceedingJoinPoint pjp) throws Throwable {
        for (int attempt = 1; ; attempt++) {
            try {
                return pjp.proceed();
            } catch (RuntimeException ex) {
                if (attempt >= MAX_ATTEMPTS || !isTransient(ex)) {
                    throw ex;
                }
                log.debug("Transient MongoDB transaction error in {} (attempt {}), retrying",
                        pjp.getSignature().toShortString(), attempt);
                // full-jitter exponential backoff: two writers that collided rarely collide again
                long cap = Math.min(MAX_BACKOFF_MS, 5L << attempt);
                Thread.sleep(ThreadLocalRandom.current().nextLong(1, cap + 1));
            }
        }
    }

    static boolean isTransient(Throwable ex) {
        for (Throwable t = ex; t != null; t = t.getCause()) {
            if (t instanceof MongoException me
                    && (me.hasErrorLabel(MongoException.TRANSIENT_TRANSACTION_ERROR_LABEL) || me.getCode() == 112)) {
                return true;
            }
        }
        return false;
    }
}
