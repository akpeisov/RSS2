package kz.home.RelaySmartSystems.model;

import kz.home.RelaySmartSystems.model.entity.Controller;
import kz.home.RelaySmartSystems.model.entity.User;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Date;

@Setter
@Getter
public class WSSession {
    private WebSocketSession session;
//    private String controllerId;
//    private String type;

    private LocalDateTime connectionDate;
    private String clientIP;
    private boolean authorized = false;
    private Date lastSend = new Date();
    private Long lastPongTime;

    // for web
    private User user;
    private Object obj;

    // for controllers
    private String mac;
    private Controller controller;

    public WSSession(WebSocketSession session, String clientIP) {
        this.session = session;
        this.connectionDate = LocalDateTime.now();
        this.clientIP = clientIP;
    }

////    public String getControllerId() {
////        return controllerId == null ? null : controllerId.toUpperCase();
////    }
////
////    public void setControllerId(String controllerId) {
////        this.controllerId = controllerId.toUpperCase();
////    }
////
//    public String getType() {
//        return type == null ? null : type.toLowerCase();
//    }

    public boolean isExpired() {
        Date now = new Date();
        return now.getTime() - lastSend.getTime() > 55 * 1000;
    }

    public String sendMessage(WebSocketMessage<?> message) {
        lastSend = new Date();
        try {
            session.sendMessage(message);
            return "OK";
        } catch (IOException e) {
            return e.getLocalizedMessage();
        }
    }

    public String getId() {
        return session.getId();
    }

    public void close(String goodbyeMessage) {
        try {
            session.sendMessage(new TextMessage(goodbyeMessage));
            session.close();
        } catch (Exception e) {
        }
    }

    public void close(BinaryMessage goodbyeMessage) {
        try {
            session.sendMessage(goodbyeMessage);
            session.close();
        } catch (Exception e) {
        }
    }
}
