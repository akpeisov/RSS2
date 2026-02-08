package kz.home.RelaySmartSystems.service;

import kz.home.RelaySmartSystems.model.entity.Session;
import kz.home.RelaySmartSystems.model.entity.SessionMessage;
import kz.home.RelaySmartSystems.repository.SessionMessageRepository;
import kz.home.RelaySmartSystems.repository.SessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SessionService {
    private final SessionRepository sessionRepository;
    private final SessionMessageRepository sessionMessageRepository;
    private static final Logger logger = LoggerFactory.getLogger(SessionService.class);
    @Value("${audit.logMessages}")
    private boolean enableLog;

    public SessionService(SessionRepository sessionRepository,
                          SessionMessageRepository sessionMessageRepository) {
        this.sessionRepository = sessionRepository;
        this.sessionMessageRepository = sessionMessageRepository;
    }

    public void addSession(String sessionId, String remoteAddr) {
        Session session = new Session();
        session.setSessionId(sessionId);
        session.setRemoteAddress(remoteAddr);
        session.setStartDate(new java.util.Date());
        session.setStatus("ACTIVE");
        sessionRepository.save(session);
    }

    public void endSession(String sessionId, String message) {
        Session session = sessionRepository.findSessionIdBySessionId(sessionId);
        if (session != null) {
            session.setEndDate(new java.util.Date());
            session.setStatus("FINISHED");
            session.setMessage(message);
            sessionRepository.save(session);
        }
    }

    public void endAllSessions() {
        int count = sessionRepository.endAllSessions();
        logger.debug("{} sessions marked as TERMINATED", count);
    }

    public void updateLastActive(String sessionId) {
        Session session = sessionRepository.findSessionIdBySessionId(sessionId);
        if (session != null) {
            session.setLastActiveDate(new java.util.Date());
            sessionRepository.save(session);
        }
    }

    public void setUsername(String sessionId, String username) {
        Session session = sessionRepository.findSessionIdBySessionId(sessionId);
        if (session != null) {
            session.setUsername(username);
            session.setType("web");
            sessionRepository.save(session);
        }
    }

    public void setMac(String sessionId, String mac) {
        Session session = sessionRepository.findSessionIdBySessionId(sessionId);
        if (session != null) {
            session.setMac(mac);
            session.setType("controller");
            sessionRepository.save(session);
        }
    }

    public void storeMessage(String sessionId, String message) {
        if (message == null || !enableLog || message.isEmpty()) {
            return;
        }
        Session session = sessionRepository.findSessionIdBySessionId(sessionId);
        SessionMessage sessionMessage = new SessionMessage();
        sessionMessage.setSession(session);
        sessionMessage.setTimestamp(new java.util.Date());
        sessionMessage.setMessage(message.substring(1, Math.min(message.length(), 2000)));
        sessionMessageRepository.save(sessionMessage);
    }
}
