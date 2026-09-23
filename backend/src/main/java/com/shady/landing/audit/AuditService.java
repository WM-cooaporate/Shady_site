package com.shady.landing.audit;

import com.shady.landing.audit.dto.AuditLogResponse;
import com.shady.landing.common.web.ClientIpResolver;
import com.shady.landing.common.web.PageResponse;
import com.shady.landing.security.CurrentAdmin;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Writes audit entries. Joins the caller's transaction, so an admin change and its audit entry commit or
 * roll back together; called outside a transaction (e.g. a failed login) it commits on its own.
 */
@Service
public class AuditService {

    private static final int MAX_ACTOR = 254;

    private final AuditLogRepository repository;
    private final ClientIpResolver clientIpResolver;
    private final Clock clock;

    public AuditService(AuditLogRepository repository, ClientIpResolver clientIpResolver, Clock clock) {
        this.repository = repository;
        this.clientIpResolver = clientIpResolver;
        this.clock = clock;
    }

    /** Records an action by the currently authenticated admin (create / update / delete). */
    @Transactional
    public void recordAdminAction(AuditAction action, String entityType, Object entityId, Map<String, Object> details) {
        record(action, CurrentAdmin.email().orElse("unknown"), entityType,
                entityId == null ? null : String.valueOf(entityId), details);
    }

    @Transactional
    public void record(AuditAction action, String actor, String entityType, String entityId,
                       Map<String, Object> details) {
        String safeActor = actor == null || actor.isBlank() ? "anonymous" : truncate(actor, MAX_ACTOR);
        repository.save(new AuditLog(Instant.now(clock), safeActor, action, entityType, entityId, currentIp(),
                details == null || details.isEmpty() ? null : details));
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> list(AuditAction action, Pageable pageable) {
        Page<AuditLog> page = action == null ? repository.findAll(pageable) : repository.findByAction(action, pageable);
        return PageResponse.of(page, AuditLogResponse::from);
    }

    private String currentIp() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            HttpServletRequest request = attributes.getRequest();
            return truncate(clientIpResolver.resolve(request), 45);
        }
        return null;
    }

    private static String truncate(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }
}
