package com.cankhata.service;

import com.cankhata.dao.AuditDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Writes the audit trail. A failure to write the audit row must never break the user's real action, so it is only logged. */
public class AuditService {
    private static final Logger log = LoggerFactory.getLogger(AuditService.class);
    private final AuditDao dao;

    public AuditService(AuditDao dao) {
        this.dao = dao;
    }

    public void log(Long userId, String action, String entity, Long entityId, String detail, String ip) {
        try {
            dao.insert(userId, action, entity, entityId, detail, ip);
        } catch (RuntimeException e) {
            log.warn("Could not write audit row {} {}: {}", action, entity, e.getMessage());
        }
    }
}
