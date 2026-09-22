package com.shady.landing.audit.web;

import com.shady.landing.audit.AuditAction;
import com.shady.landing.audit.AuditService;
import com.shady.landing.audit.dto.AuditLogResponse;
import com.shady.landing.common.config.OpenApiConfig;
import com.shady.landing.common.web.PageRequests;
import com.shady.landing.common.web.PageResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/audit-log")
@SecurityRequirement(name = OpenApiConfig.BEARER)
public class AdminAuditController {

    private final AuditService auditService;

    public AdminAuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    public PageResponse<AuditLogResponse> list(@RequestParam(required = false) AuditAction action,
                                               @RequestParam(required = false) Integer page,
                                               @RequestParam(required = false) Integer size) {
        return auditService.list(action, PageRequests.of(page, size, Sort.by(Sort.Direction.DESC, "occurredAt")));
    }
}
