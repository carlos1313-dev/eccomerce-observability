package com.ecommerce.audit;

import com.ecommerce.entity.AuditLog;
import com.ecommerce.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor @Slf4j
public class AuditService {
    private final AuditLogRepository repo;
    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    public AuditService(AuditLogRepository repo) {
        this.repo = repo;
    }
    
    @Async @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String action, String entityType, Long entityId,
                    String userEmail, String details, AuditLog.Outcome outcome) {
        try {
            repo.save(AuditLog.builder().action(action).entityType(entityType)
                    .entityId(entityId).userEmail(userEmail).details(details).outcome(outcome).build());
        } catch (Exception e) { log.error("Error audit log: {}", e.getMessage()); }
    }

    public void logSuccess(String a, String t, Long id, String u, String d) {
        log(a, t, id, u, d, AuditLog.Outcome.SUCCESS); }
    public void logFailure(String a, String t, Long id, String u, String d) {
        log(a, t, id, u, d, AuditLog.Outcome.FAILURE); }
    public void logDenied(String a, String t, Long id, String u, String d) {
        log(a, t, id, u, d, AuditLog.Outcome.DENIED); }
}
