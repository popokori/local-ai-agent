package mr.popo.localaiagent.audit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mr.popo.localaiagent.audit.domain.AuditAction;
import mr.popo.localaiagent.audit.domain.AuditLog;
import mr.popo.localaiagent.audit.repository.AuditLogRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Persiste les actions sensibles en DB pour audit + trace SLF4J.
 * Appels explicites depuis les services (Phase 1) ; AOP @Audited prévu en Phase 5.
 * Asynchrone pour ne pas pénaliser la latence des requêtes utilisateur.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository repository;

    @Async("applicationTaskExecutor")
    public void log(AuditAction action, Long userId, Long sessionId,
                    Map<String, Object> payload, boolean success, Integer durationMs) {
        try {
            AuditLog entry = new AuditLog();
            entry.setAction(action);
            entry.setUserId(userId);
            entry.setSessionId(sessionId);
            entry.setPayloadJson(payload);
            entry.setSuccess(success);
            entry.setDurationMs(durationMs);
            repository.save(entry);

            log.info("audit action={} userId={} sessionId={} success={} durationMs={}",
                    action, userId, sessionId, success, durationMs);
        } catch (Exception ex) {
            // L'audit ne doit jamais faire planter une requête utilisateur.
            log.error("Failed to persist audit entry", ex);
        }
    }
}
