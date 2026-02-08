package kz.home.RelaySmartSystems.service;

import kz.home.RelaySmartSystems.model.alice.AliceRequestLog;
import kz.home.RelaySmartSystems.repository.AliceRequestLogRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AliceRequestLogService {
    private final AliceRequestLogRepository aliceRequestLogRepository;
    public AliceRequestLogService(AliceRequestLogRepository aliceRequestLogRepository) {
        this.aliceRequestLogRepository = aliceRequestLogRepository;
    }

    public UUID writeLog(String method, String sourceIP, String requestId, String username, String token, String request) {
        AliceRequestLog aliceRequestLog = new AliceRequestLog();
        aliceRequestLog.setMethod(method);
        aliceRequestLog.setRequestId(requestId);
        aliceRequestLog.setToken(token);
        aliceRequestLog.setUsername(username);
        aliceRequestLog.setRequest(request);
        //aliceRequestLog.setResponse(response);
        aliceRequestLog.setSourceIP(sourceIP);
        aliceRequestLog = aliceRequestLogRepository.save(aliceRequestLog);
        return aliceRequestLog.getUuid();
    }

    public void setResponse(UUID uuid, String response) {
        if (uuid == null || response == null)
            return;
        AliceRequestLog aliceRequestLog = aliceRequestLogRepository.findById(uuid).orElse(null);
        if (aliceRequestLog == null)
            return;
        aliceRequestLog.setResponse(response);
        aliceRequestLogRepository.save(aliceRequestLog);
    }
}