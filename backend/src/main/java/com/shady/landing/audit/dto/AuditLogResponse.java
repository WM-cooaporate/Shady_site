package com.shady.landing.audit.dto;

import com.shady.landing.audit.AuditAction;
import com.shady.landing.audit.AuditLog;
import java.time.Instant;
import java.util.Map;

public record AuditLogResponse(
        Long id,
        Instant occurredAt,
        String actor,
        AuditAction action,
        String entityType,
        String entityId,
        String ipAddress,
        Map<String, Object> details) {

    public static AuditLogResponse from(AuditLog log) {
        return new AuditLogResponse(log.getId(), log.getOccurredAt(), log.getActor(), log.getAction(),
                log.getEntityType(), log.getEntityId(), log.getIpAddress(), log.getDetails());
    }
}
