package com.uit.petrescueapi.domain.repository;

import com.uit.petrescueapi.domain.entity.AuditLog;

/**
 * Domain repository contract for the AuditLog entity.
 */
public interface AuditLogRepository {

    AuditLog save(AuditLog auditLog);
}
