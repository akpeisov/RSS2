package kz.home.RelaySmartSystems.filters;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

public class IpHandshakeInterceptor implements HandshakeInterceptor {

    public static final String CLIENT_IP_ADDRESS_KEY = "clientIpAddress";

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        String ipAddress = null;

        // 1. X-Real-IP (часто устанавливается nginx)
        ipAddress = request.getHeaders().getFirst("X-Real-IP");
        if (isEmptyOrUnknown(ipAddress)) {
            // 2. X-Forwarded-For может содержать список адресов: client, proxy1, proxy2
            String xff = request.getHeaders().getFirst("X-Forwarded-For");
            if (!isEmptyOrUnknown(xff)) {
                // берём первый адрес в списке
                String[] parts = xff.split(",");
                if (parts.length > 0) {
                    ipAddress = parts[0].trim();
                }
            }
        }

        if (isEmptyOrUnknown(ipAddress)) {
            request.getRemoteAddress();
            ipAddress = request.getRemoteAddress().getAddress().getHostAddress();
        }

        attributes.put(CLIENT_IP_ADDRESS_KEY, ipAddress);
        return true;
    }

    private boolean isEmptyOrUnknown(String s) {
        return s == null || s.trim().isEmpty() || "unknown".equalsIgnoreCase(s.trim());
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
    }
}
