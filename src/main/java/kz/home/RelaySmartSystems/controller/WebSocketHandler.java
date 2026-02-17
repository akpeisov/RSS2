package kz.home.RelaySmartSystems.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kz.home.RelaySmartSystems.Utils;
import kz.home.RelaySmartSystems.VersionPrinter;
import kz.home.RelaySmartSystems.filters.IpHandshakeInterceptor;
import kz.home.RelaySmartSystems.filters.JwtAuthorizationFilter;
import kz.home.RelaySmartSystems.model.*;
import kz.home.RelaySmartSystems.model.def.*;
import kz.home.RelaySmartSystems.model.dto.*;
import kz.home.RelaySmartSystems.model.entity.Controller;
import kz.home.RelaySmartSystems.model.entity.User;
import kz.home.RelaySmartSystems.model.entity.CModel;
import kz.home.RelaySmartSystems.model.entity.relaycontroller.RCDeviceInfo;
import kz.home.RelaySmartSystems.model.entity.relaycontroller.RelayController;
import kz.home.RelaySmartSystems.service.ControllerService;
import kz.home.RelaySmartSystems.service.RelayControllerService;
import kz.home.RelaySmartSystems.service.SessionService;
import kz.home.RelaySmartSystems.service.UserService;
import kz.home.RelaySmartSystems.model.EErrorCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;


import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static kz.home.RelaySmartSystems.model.mapper.BConfigMapper.mapAction;

@Component
public class WebSocketHandler extends AbstractWebSocketHandler {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketHandler.class);
    private static final ArrayList<WSSession> wsSessions = new ArrayList<>();
    private final ControllerService controllerService;
    private final RelayControllerService relayControllerService;
    private final JwtAuthorizationFilter jwtAuthorizationFilter;
    private final UserService userService;
    private final SessionService sessionService;

    public WebSocketHandler(ControllerService controllerService,
                            RelayControllerService relayControllerService,
                            JwtAuthorizationFilter jwtAuthorizationFilter,
                            UserService userService,
                            SessionService sessionService) {
        this.controllerService = controllerService;
        this.relayControllerService = relayControllerService;
        this.jwtAuthorizationFilter = jwtAuthorizationFilter;
        this.userService = userService;
        this.sessionService = sessionService;

        sessionService.endAllSessions();
    }

    @Override
    protected void handlePongMessage(WebSocketSession session, PongMessage message) throws Exception {
        WSSession wsSession = getWSSession(session);
        if (wsSession != null) {
            wsSession.setLastPongTime(System.currentTimeMillis());
        }
        //sessionService.updateLastActive(session.getId());
        //logger.info("pong message {} sess {}", message.getPayload(), session.getId());
        super.handlePongMessage(session, message);
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
        // for controllers
        logger.warn("Binary message {} bytes. {}", message.getPayloadLength(), printBinaryMessage(message));
        sessionService.updateLastActive(session.getId());
        sessionService.storeMessage(session.getId(), printBinaryMessage(message));
        WSSession wsSession = getWSSession(session);
        if (wsSession == null) {
            logger.error("wsSession not found for binary message!");
            session.sendMessage(binMessage("E", EErrorCodes.E_ERROR.getValue()));
            session.close();
            return;
        }

        controllerService.updateLastSeen(wsSession.getController());

        //MGC MSGTYPE PAYLOAD CRC
        if (!checkCRC(message)) {
            logger.error("Wrong crc");
            wsSession.sendMessage(binMessage("E", EErrorCodes.E_CRC_ERROR.getValue()));
            return;
        }
        message.getPayload().rewind();
        int cmd = message.getPayload().get(1) & 0xFF;
        if (!wsSession.isAuthorized() && cmd != 'H') {
            logger.error("Not authorized");
            wsSession.sendMessage(binMessage("E", EErrorCodes.E_NOT_AUTHRORIZED.getValue()));
            return;
        }

        switch (cmd) {
            case 'H':
                checkHello(message, wsSession);
                break;
            case 'I':
                // info
                infoProcess(message, wsSession);
                break;
            case 'N':
                // new node (mac, model)
                newNodeEvent(message, wsSession);
                break;
            case 'E':
                // event (mac, type[i/o], id, state)
                ioEvent(message, wsSession);
                break;
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // for web
        sessionService.updateLastActive(session.getId());
        sessionService.storeMessage(session.getId(), message.getPayload());
        WSSession wsSession = getWSSession(session);
        if (wsSession == null) {
            logger.error("wsSession not found!");
            session.sendMessage(new TextMessage(errorMessage("Good bye!")));
            session.close();
            return;
        }

        String payload = message.getPayload();
        logger.debug(payload);
        byte[] json;
        WSTextMessage wsTextMessage;
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            wsTextMessage = objectMapper.readValue(payload, WSTextMessage.class);
            json = objectMapper.writeValueAsBytes(wsTextMessage.getPayload());
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage());
            wsSession.close(errorMessage("Wrong message structure"));
            return;
        }

        String type = wsTextMessage.getType();
        if (type == null) {
            wsSession.close(errorMessage("Type is null"));
            return;
        }
        if (!wsSession.isAuthorized() && !type.equals("HELLO") && !type.equals("DEVICECONFIG")) {
            wsSession.close(errorMessage("Good bye!"));
            return;
        }

        switch (type) {
            case "HELLO":
                Hello hello = objectMapper.readValue(json, Hello.class);
                helloProcess(hello, wsSession);
                break;

            case "USERDEVICES":
                getUserDevices(wsSession);
                break;

            case "DEVICECONFIG":
                RCConfigDTO rcConfigDTO = objectMapper.readValue(json, RCConfigDTO.class);
                getDeviceConfig(rcConfigDTO, wsSession);
                break;

            case "ACTION":
                ActionDTO actionDTO = objectMapper.readValue(json, ActionDTO.class);
                sendAction(actionDTO, wsSession, wsTextMessage);
                break;

            case "UPDATEOUTPUT":
                RCOutputDTO rcUpdateOutputDTO = objectMapper.readValue(json, RCOutputDTO.class);
                rcUpdateOutput(rcUpdateOutputDTO, wsSession);
                break;

            case "UPDATEINPUT":
                RCInputDTO rcUpdateInputDTO = objectMapper.readValue(json, RCInputDTO.class);
                rcInputUpdate(rcUpdateInputDTO, wsSession);
                break;

            case "REQUESTFORLINK":
                LinkRequest linkRequest = objectMapper.readValue(json, LinkRequest.class);
                requestForLink(linkRequest, wsSession);
                break;

            case "COMMAND":
                Command command = objectMapper.readValue(json, Command.class);
                processCommand(command, wsSession);
                break;

            default:
                logger.error("Type {}. Unknown message {}", type, wsTextMessage.getPayload().toString());
                session.sendMessage(new TextMessage(errorMessage("Unknown message")));
                break;
        }
    }

    private void helloProcess(Hello hello, WSSession session) {
        TokenData tokenData = jwtAuthorizationFilter.validateToken(hello.getToken(), hello.getType());
        if (tokenData.getErrorText() != null) {
            logger.error("Token error {}", tokenData.getErrorText());
            session.close(errorMessage(String.format("Token error. %s. Closing connection.", tokenData.getErrorText())));
            return;
        }

        if (tokenData.getUsername() != null) {
            sessionService.setUsername(session.getId(), tokenData.getUsername());
            User user = userService.findByUsername(tokenData.getUsername());
            if (user != null) {
                session.setUser(user);
            } else {
                logger.error("User by username not found {}", tokenData.getUsername());
                session.close(errorMessage("User not found!"));
                return;
            }
        } else {
            session.close(errorMessage("No identifiers!"));
            return;
        }
        session.setAuthorized(true);
        session.sendMessage(new TextMessage(getCmdMessage("AUTHORIZED")));
        logger.info("Web client connected. Username {}", session.getUser().getUsername());
    }

    private void getUserDevices(WSSession session) throws IOException {
        List<Object> devices = controllerService.getUserDevices(session.getUser());
        session.sendMessage(new TextMessage(message("USERDEVICES", devices)));
        logger.debug("user devices sent to {}", session.getUser().getUsername());
    }

    private void getDeviceConfig(RCConfigDTO rcConfigDTO, WSSession session) {
        String res = relayControllerService.saveConfig(rcConfigDTO);
        if ("OK".equalsIgnoreCase(res))
            session.sendMessage(new TextMessage(successMessage("Saved successfully")));
        else
            session.sendMessage(new TextMessage(errorMessage(res)));
    }

    private void sendAction(ActionDTO actionDTO, WSSession session, WSTextMessage wsTextMessage) {
        // отправка действия на контроллер
        User user = controllerService.findControllerOwner(actionDTO.getMac());
        if ((session.getUser() != null && session.getUser().isAdmin()) ||
                (user != null && user.equals(session.getUser()))) {
            logger.debug("Sending message {} to controller {}", actionDTO.getAction(), actionDTO.getMac());
            BinaryMessage msg = relayControllerService.getActionMessage(actionDTO);
            if (msg != null) {
                sendMessageToController(actionDTO.getMac(), msg);
            }
        } else {
            logger.debug("Action message to {} with incorrect owner {}",
                    actionDTO.getMac(), session.getUser() != null ? session.getUser().getUsername() : "no_user");
        }
    }

    private void rcUpdateOutput(RCOutputDTO outputDTO, WSSession session) {
        // обновление выхода контроллера
        // обновление только в БД, конфиг потом руками на контроллер
        String res = relayControllerService.updateOutput(outputDTO);
        if ("OK".equalsIgnoreCase(res))
            session.sendMessage(new TextMessage(successMessage("Saved successfully")));
        else
            session.sendMessage(new TextMessage(errorMessage(res)));
    }

    private void rcInputUpdate(RCInputDTO inputDTO, WSSession session) {
        // обновление входа контроллера
        String res = relayControllerService.updateInput(inputDTO);
        if ("OK".equalsIgnoreCase(res))
            session.sendMessage(new TextMessage(successMessage("Saved successfully")));
        else
            session.sendMessage(new TextMessage(errorMessage(res)));
    }

    private void requestForLink(LinkRequest linkRequest, WSSession session) {
        if (linkRequest.getMac() != null) {
            Controller controller = controllerService.findController(linkRequest.getMac());
            if (controller != null) {
                // проверить готов ли контроллер к линку
                if (controller.isLinked()) {
                    session.sendMessage(new TextMessage(errorMessage("Controller already linked")));
                } else {
                    // проверить онлайн ли сейчас контроллер
                    if (!isControllerOnline(controller)) {
                        session.sendMessage(new TextMessage(errorMessage("Controller is offline. Make sure that is powered on and connect to internet.")));
                    } else {
                        Map<String, Object> payld = new HashMap<>();
                        //payld.put("message", 123);
                        payld.put("controllertype", controller.getType());
                        session.sendMessage(new TextMessage(WSTextMessage.send("LINK", payld)));
                        // find controller and wait link event from it
                        // make temporary flag
                        // TODO: fix it session.setControllerId(linkRequest.getMac());
                        session.setObj("linkRequested"); // set curtime
                    }
                }
            } else {
                session.sendMessage(new TextMessage(errorMessage("Controller not found")));
            }
        } else if ("linkRequestTimeout".equalsIgnoreCase(linkRequest.getEvent())) {
            session.setObj(null);
        } else {
            session.sendMessage(new TextMessage(errorMessage("Controller not found")));
        }
    }

    private void processCommand(Command command, WSSession wsSession) {
        logger.debug("Command {}, mac {}", command.getCommand(), command.getMac());
        if (!isControllerOnline(command.getMac())) {
            wsSession.sendMessage(new TextMessage(errorMessage("Controller offline")));
            return;
        }
        BinaryMessage msg = null;
        if ("toggleSendLogs".equalsIgnoreCase(command.getCommand())) {
            msg = relayControllerService.getCmdMessage("L");
        } else if ("startOTA".equalsIgnoreCase(command.getCommand())) {
            msg = relayControllerService.getCmdMessage("O");
        } else if ("INFO".equalsIgnoreCase(command.getCommand())) {
            msg = relayControllerService.getCmdMessage("I");
        } else if ("REBOOT".equalsIgnoreCase(command.getCommand())) {
            msg = relayControllerService.getCmdMessage("R");
        } else if ("UPLOADCONFIG".equalsIgnoreCase(command.getCommand())) {
            msg = relayControllerService.makeBConfig(command.getMac());
        } else if ("DELETE".equalsIgnoreCase(command.getCommand())) {
            String res = controllerService.deleteController(command.getMac());
            if ("OK".equalsIgnoreCase(res)) {
                wsSession.sendMessage(new TextMessage(successMessage("Controller deleted")));
            } else {
                wsSession.sendMessage(new TextMessage(errorMessage(res)));
            }
            return;
        }
        if (msg != null) {
            String res = sendMessageToController(command.getMac(), msg);
            if ("OK".equalsIgnoreCase(res))
                wsSession.sendMessage(new TextMessage(successMessage("Successfully sent")));
            else
                wsSession.sendMessage(new TextMessage(errorMessage(res)));
        } else {
            logger.error("processCommand error, msg is null");
        }
    }

    private String errorMessage(String message) {
        Map<String, Object> payld = new HashMap<>();
        payld.put("message", message);
        return WSTextMessage.send("ERROR", payld);
    }

    private String successMessage(String message) {
        Map<String, Object> payld = new HashMap<>();
        payld.put("message", message);
        return WSTextMessage.send("SUCCESS", payld);
    }

    private String message(String type, Object message) {
        return WSTextMessage.send(type, message);
    }

    private Object controllerStatus(String mac, String status) {
        Map<String, Object> payld = new HashMap<>();
        payld.put("mac", mac);
        payld.put("status", status);
        return payld;
    }

    private String getCmdMessage(String cmd) {
        return WSTextMessage.send(cmd, null);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String clientIpAddress = (String) session.getAttributes().get(IpHandshakeInterceptor.CLIENT_IP_ADDRESS_KEY);
        WSSession wsSession = new WSSession(session, clientIpAddress);
        wsSessions.add(wsSession);
        logger.debug("New client connected with ID {} client IP {}", session.getId(), clientIpAddress);
        sessionService.addSession(session.getId(), clientIpAddress);
        HttpHeaders headers = session.getHandshakeHeaders();
        String userAgent = headers.getFirst(HttpHeaders.USER_AGENT);
        if (userAgent != null && userAgent.contains("ESP32")) {
            // send hello from server
            session.sendMessage(getHelloServerMessage());
        }
        super.afterConnectionEstablished(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Throwable lastError = (Throwable) session.getAttributes().get("lastError");
        String message;
        if (lastError != null) {
            message = lastError.getMessage();
        } else {
            message = status.getReason();
        }
        sessionService.endSession(session.getId(), String.format("%d%s", status.getCode(), message == null ? "" : ", " + message));
        // find current controller and set offline
        String mac = getControllerIdForWSSession(session);
        if (mac != null) {
            controllerService.setControllerOffline(mac);
            sendMessageToWebUsers(mac, message("STATUS", controllerStatus(mac, "offline")));
        }
        logger.info("Client {} disconnected. Code {}, reason {}", mac == null ? "web " + session.getId() : "controller " + mac, status.getCode(), status.getReason());
        wsSessions.removeIf(s -> s.getSession().equals(session));
        super.afterConnectionClosed(session, status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        logger.error("handleTransportError with ID {} {}", session.getId(), exception.getMessage());
        session.getAttributes().put("lastError", exception);
    }

    public BinaryMessage getHelloServerMessage() {
        byte[] data = new byte[11];
        data[0] = (byte) 0xAA;
        data[1] = (byte) 0xBB;

        String version = VersionPrinter.getAppVersion();
        if (version == null) {
            version = "0.0.0";
        }
        String[] parts = version.split("\\.");
        data[2] = (byte) Integer.parseInt(parts[0]);
        data[3] = (byte) Integer.parseInt(parts[1]);
        data[4] = (byte) Integer.parseInt(parts[2]);

        // get current datetime as unix timestamp and write to data
        long timestamp = System.currentTimeMillis() / 1000L;
        data[5] = (byte) (timestamp & 0xFF);
        data[6] = (byte) ((timestamp >> 8) & 0xFF);
        data[7] = (byte) ((timestamp >> 16) & 0xFF);
        data[8] = (byte) ((timestamp >> 24) & 0xFF);

        int crc = Utils.crc16(data, 9);
        data[9] = (byte) (crc & 0xFF);
        data[10] = (byte) ((crc >> 8) & 0xFF);

        return new BinaryMessage(data);
    }

    private User getUserForSession(WebSocketSession targetSession) {
        Optional<WSSession> session = wsSessions.stream()
                .filter(s -> s.getSession().equals(targetSession))
                .findFirst();
        return session.map(WSSession::getUser).orElse(null);
    }

    public String sendMessageToController(String mac, BinaryMessage message) {
        if (mac == null)
            return "MAC_NULL";
        return wsSessions.stream()
            .filter(session -> mac.equalsIgnoreCase(session.getMac()))
            .findFirst().map(session -> {
                session.sendMessage(message);
                return "OK";
            }).orElse("NOT_FOUND");
    }

    private void sendMessageToWebUsers(String mac, String message) {
        // найти все текущие пользовательские сессии, к которым относится данный мак
        User user = relayControllerService.getUser(mac);
        wsSessions.stream().filter(session -> session.getUser() != null).toList().forEach(
                session -> {
                    if (session.getUser().isAdmin() || session.getUser().equals(user)) {
                        session.sendMessage(new TextMessage(message));
                    }
                }
        );
    }

    public void sendMessageToWebUser(User user, String message) {
        // Отправка сообщения во все пользовательские сессии
//        logger.debug("sendMessageToWebUser. User {}. Message {}", user == null ? "no_user" : user.getUsername(), message);
//        for (WSSession session: wsSessions) {
//            if ("web".equalsIgnoreCase(session.getType()) &&
//                    (session.getUser() != null && (session.getUser().isAdmin() || session.getUser().equals(user)))) {
//                try {
//                    if (session.getSession().isOpen()) {
//                        session.sendMessage(new TextMessage(message));
//                    }
//                } catch (IOException e) {
//                    logger.error(e.getLocalizedMessage());
//                }
//            }
//        }
    }

    private WSSession findSessionForLinkRequest(String mac) {
        // определить есть ли запрос на линковку в текущих сессиях фронта
//        for (WSSession wsSession : wsSessions) {
//            if ("web".equals(wsSession.getType()) &&
//                wsSession.getControllerId() != null &&
//                wsSession.getControllerId().equalsIgnoreCase(mac) &&
//                "linkRequested".equalsIgnoreCase((String)wsSession.getObj()))  {
//                return wsSession;
//            }
//        }
        return null;
    }

    private boolean isControllerOnline(String mac) {
        Controller c = controllerService.findController(mac);
        return isControllerOnline(c);
    }

    private boolean isControllerOnline(Controller c) {
        if (c == null)
            return false;
        return "online".equalsIgnoreCase(c.getStatus());
    }

    private String getControllerIdForWSSession(WebSocketSession targetSession) {
        Optional<WSSession> session = wsSessions.stream()
                .filter(s -> s.getSession().equals(targetSession))
                .findFirst();
        return session.map(WSSession::getMac).orElse(null);
    }

    @Scheduled(fixedRate = 10000)
    private void alive() throws IOException {
        for (WSSession wsSession : wsSessions) {
            wsSession.sendMessage(new PingMessage());
            /*
            if (wsSession != null &&
                    wsSession.getSession().isOpen() &&
                    wsSession.isExpired() &&
                    wsSession.getUser() != null) {
                String date = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(new Date());
                wsSession.sendMessage(new TextMessage(AlertMessage.makeAlert(String.format("alive %s", date))));
            }

             */
        }
    }

    @Scheduled(fixedRate = 20000)
    private void serviceTask() {
        controllerService.setInactiveOffline();
    }

    private WSSession getWSSession(WebSocketSession session) {
        for (WSSession wsSession : wsSessions) {
            if (wsSession != null && wsSession.getSession().equals(session)) {
                return wsSession;
            }
        }
        return null;
    }

    private String printBinaryMessage(BinaryMessage msg) {
        ByteBuffer payload = msg.getPayload();
        byte[] bytes = new byte[payload.remaining()];
        payload.rewind();
        payload.get(bytes);
        return java.util.HexFormat.of().withUpperCase().formatHex(bytes);
    }

    private BinaryMessage binMessage(String type, int err) {
        byte[] msg = {(byte) 0xAA, (byte) type.charAt(0), (byte) err};
        // add crc to end of packet
        int crc = Utils.crc16(msg, msg.length);
        byte crcLow = (byte) (crc & 0xFF);
        byte crcHigh = (byte) ((crc >> 8) & 0xFF);
        msg = Arrays.copyOf(msg, msg.length + 2);
        msg[msg.length - 2] = crcLow;
        msg[msg.length - 1] = crcHigh;
        return new BinaryMessage(msg);
    }

    private boolean checkCRC(BinaryMessage msg) {
        ByteBuffer buffer = msg.getPayload();
        int length = msg.getPayloadLength();
        if (length < 4) return false;
        buffer.rewind();
        if ((buffer.get(0) & 0xFF) != 0xAA) return false;
        byte[] data = new byte[length - 2];
        buffer.get(data);
        int receivedCRC = (buffer.get() & 0xFF) | ((buffer.get() & 0xFF) << 8);
        int crc = Utils.crc16(data, length-2);
        logger.info("CRC calculated: {}, received: {}", Integer.toHexString(crc), Integer.toHexString(receivedCRC));
        return crc == receivedCRC;
    }

    private void checkHello(BinaryMessage msg, WSSession session) {
        ByteBuffer buffer = msg.getPayload();
        int length = msg.getPayloadLength();

        int start = 2;
        int end = length - 2;
        int dataLength = end - start;

        if (dataLength <= 0) {
            session.sendMessage(binMessage("H", EErrorCodes.E_HELLO_ERROR.getValue()));
            return;
        }

        byte[] data = new byte[dataLength];
        buffer.position(start);
        buffer.get(data);

        for (byte b : data) {
            if (!isPrintable(b)) {
                session.sendMessage(binMessage("H", EErrorCodes.E_HELLO_ERROR.getValue()));
                return;
            }
        }

        String jwt = new String(data, StandardCharsets.UTF_8);
        TokenData tokenData = jwtAuthorizationFilter.validateToken(jwt, "controller");
        if (tokenData.getErrorText() != null) {
            logger.error("Token error {}", tokenData.getErrorText());
            session.close(binMessage("H", EErrorCodes.E_TOKEN_ERROR.getValue()));
            return;
        }

        if (tokenData.getMac() != null) {
            session.setMac(tokenData.getMac());
            // check for exists and create if need
            try {
                relayControllerService.checkCreateRC(tokenData.getMac(), CModel.valueOf(tokenData.getModel()));
                session.setAuthorized(true);
                logger.info("Controller {} authorized successfully", session.getMac());
                session.sendMessage(binMessage("H", 0)); // may be auth ack?
                controllerService.setControllerOnline(session.getMac());
                session.setController(controllerService.findController(session.getMac()));
                sendMessageToWebUsers(session.getMac(), message("STATUS", controllerStatus(session.getMac(), "online")));
            } catch (Exception e) {
                logger.error("checkCreateRC has error {}", e.getLocalizedMessage());
                session.close(binMessage("H", EErrorCodes.E_SERVER_ERROR.getValue()));
            }
        } else {
            session.close(binMessage("H", EErrorCodes.E_NOMAC_ERROR.getValue()));
        }
    }

    private boolean isPrintable(byte b) {
        int value = b & 0xFF;
        return (value >= 32 && value <= 126) || value == 10 || value == 13;
    }

    private void newNodeEvent(BinaryMessage msg, WSSession wsSession) {
        // when new node discovered need to link nodes
        ByteBuffer buffer = msg.getPayload();
        int length = msg.getPayloadLength();

        int start = 2;
        int dataLength = length - 2 - start;

        if (dataLength != (6 + 1)) {
            wsSession.sendMessage(binMessage("N", EErrorCodes.E_NODE_ERROR.getValue()));
            return;
        }

        byte[] data = new byte[dataLength-1];
        buffer.position(start);
        buffer.get(data);

        String mac = HexFormat.of().formatHex(data).toUpperCase(); //new String(data, StandardCharsets.UTF_8);
        if (mac.length() != 12 || mac.equalsIgnoreCase(wsSession.getMac())) {
            wsSession.sendMessage(binMessage("N", EErrorCodes.E_NODE_ERROR.getValue()));
            return;
        }
        CModel model = CModel.fromInt(buffer.get() & 0xFF);

        relayControllerService.linkNodeRC(wsSession.getMac(), mac, model);
        wsSession.sendMessage(binMessage("N", 0)); // may be auth ack?
    }

    private void ioEvent(BinaryMessage msg, WSSession wsSession) {
        ByteBuffer buffer = msg.getPayload();
        int start = 2;

        byte[] data = new byte[6];
        buffer.position(start);
        buffer.get(data);

        String node = HexFormat.of().formatHex(data).toUpperCase();
        int type = buffer.get() & 0xFF;
        int id = buffer.get() & 0xFF;
        int state = buffer.get() & 0xFF;
        String sState = state == 0 ? "off" : "on";
        Map<String, Object> payld = new HashMap<>();
        payld.put("mac", node);
        payld.put("state", sState);
        if (type == 0) {
            // output
            int timer = Short.toUnsignedInt(buffer.getShort());
            payld.put("output", id);
            payld.put("timer", timer);
            relayControllerService.setOutputState(wsSession.getMac(), id, sState);
        } else if (type == 1) {
            // input
            payld.put("input", id);
            relayControllerService.setInputState(wsSession.getMac(), id, sState);
        }
        // TODO : save to db
        sendMessageToWebUsers(wsSession.getMac(), message("UPDATE", payld));
    }

    private String macToString(byte[] mac) {
        return String.format("%02X%02X%02X%02X%02X%02X", mac[0], mac[1], mac[2], mac[3], mac[4], mac[5]);
    }

    private String intToIp(int ip) {
        return (ip & 0xFF) + "." +
               ((ip >> 8) & 0xFF) + "." +
               ((ip >> 16) & 0xFF) + "." +
               ((ip >> 24) & 0xFF);
    }

    void infoProcess(BinaryMessage msg, WSSession wsSession)  {
        ByteBuffer buf = msg.getPayload();
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.position(2);

        RCDeviceInfo d = new RCDeviceInfo();
        byte[] macBytes = new byte[6];
        buf.get(macBytes);
        d.setMac(macToString(macBytes));
        d.setFreeMemory(Integer.toUnsignedLong(buf.getInt()));
        d.setUptimeRaw(Integer.toUnsignedLong(buf.getInt()));
        d.setVersion(Short.toUnsignedInt(buf.getShort()));
        d.setCurdate(Integer.toUnsignedLong(buf.getInt()));
        d.setWifiRSSI(buf.get());
        d.setEthIP(intToIp(buf.getInt()));
        d.setWifiIP(intToIp(buf.getInt()));

        byte[] rr = new byte[36];
        buf.get(rr);
        d.setResetReason(new String(rr, StandardCharsets.UTF_8).trim());
        controllerService.setControllerInfo(d);

        // rc special
        int outputsStates = buf.getInt() & 0xFFFF;
        int inputsStates = buf.getInt() & 0xFFFF;
        relayControllerService.updateIOStates((RelayController) wsSession.getController(), outputsStates, inputsStates);

        d.setNeighborCount(Byte.toUnsignedInt(buf.get()));

        for (int i = 0; i < 10; i++) {
            byte[] nmac = new byte[6];
            buf.get(nmac);
            CModel model = CModel.fromInt(Byte.toUnsignedInt(buf.get()));
            outputsStates = buf.getInt();
            inputsStates = buf.getInt();

            if (i < d.getNeighborCount()) {
                RCDeviceInfo.NeighborInfo n = new RCDeviceInfo.NeighborInfo();
                n.setMac(macToString(nmac));
                n.setModel(model);
                n.setOutputsStates(outputsStates);
                n.setInputsStates(inputsStates);
                d.getNeighbors().add(n);
                RelayController rc = relayControllerService.findRelayController(n.getMac());
                if (rc != null && !"online".equalsIgnoreCase(rc.getStatus())) {
                    relayControllerService.updateIOStates(rc, outputsStates, inputsStates);
                }
            }
        }
    }
}