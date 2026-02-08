package kz.home.RelaySmartSystems.controller;

import kz.home.RelaySmartSystems.Utils;
import kz.home.RelaySmartSystems.model.alice.AliceResponseError;
import kz.home.RelaySmartSystems.model.entity.User;
import kz.home.RelaySmartSystems.model.alice.*;
import kz.home.RelaySmartSystems.model.entity.relaycontroller.RCOutput;
import kz.home.RelaySmartSystems.repository.RCOutputRepository;
import kz.home.RelaySmartSystems.repository.UserRepository;
import kz.home.RelaySmartSystems.service.AliceRequestLogService;
import kz.home.RelaySmartSystems.service.RelayControllerService;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.HandlerMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

@RestController
@RequestMapping(value = "alice/v1.0")
public class AliceController {
    private static final Logger logger = LoggerFactory.getLogger(AliceController.class);
    private final UserRepository userRepository;
    private final AliceRequestLogService aliceRequestLogService;
    private final RCOutputRepository rcOutputRepository;
    private final WebSocketHandler webSocketHandler;
    private final RelayControllerService relayControllerService;

    @Getter
    private static class WSCheck {
        private ResponseEntity<?> responseEntity;
        private User user;
        private UUID logId;
        public boolean hasError() {
            return responseEntity != null;
        }
        public WSCheck(ResponseEntity<?> responseEntity) {
            this.responseEntity = responseEntity;
        }
        public WSCheck(User user, UUID logId) {
            this.user = user;
            this.logId = logId;
        }
    }

    private WSCheck processCheck(HttpServletRequest request, String requestId,
                                 String methodName, String requestBody) {
        //WSCheck result = new WSCheck();
        String msg = "";
        String username = (String) request.getAttribute("username");
        UUID logId = aliceRequestLogService.writeLog(methodName,
                getIp(request), requestId,
                username, (String)request.getAttribute("token"), requestBody);
        //logger.info("user devices query. Username {}", username);
        if (requestId == null || requestId.isEmpty()) {
            msg = "X-Request-Id required";
            aliceRequestLogService.setResponse(logId, msg);
            return new WSCheck(ResponseEntity.status(400).body(new AliceResponseError(requestId, msg)));
        }
        if (username == null) {
            msg = "Username is null";
            aliceRequestLogService.setResponse(logId, msg);
            return new WSCheck(ResponseEntity.status(404).body(new AliceResponseError(requestId, msg)));
        }
        User user = userRepository.findByUsername(username);
        if (user == null) {
            msg = String.format("User with username %s not found", username);
            logger.info(msg);
            aliceRequestLogService.setResponse(logId, msg);
            return new WSCheck(ResponseEntity.status(404).body(new AliceResponseError(requestId, msg)));
        }
        return new WSCheck(user, logId);
    }
/*
    private class WSCheck {
        private ResponseEntity<?> responseEntity;
        private User user;
        private final UserRepository userRepository;
        private final AliceRequestLogService aliceRequestLogService;

        public boolean hasError() {
            return responseEntity != null;
        }

        public WSCheck(UserRepository userRepository,
                       AliceRequestLogService aliceRequestLogService) {
            this.userRepository = userRepository;
            this.aliceRequestLogService = aliceRequestLogService;
        }

        public <T> ProcessedCheck<T> p(HttpServletRequest request, String requestId, T requestBody, String methodName) {

        }
    }
*/
    public AliceController(UserRepository userRepository,
                           AliceRequestLogService aliceRequestLogService,
                           RCOutputRepository rcOutputRepository,
                           WebSocketHandler webSocketHandler,
                           RelayControllerService relayControllerService) {
        this.userRepository = userRepository;
        this.aliceRequestLogService = aliceRequestLogService;
        this.rcOutputRepository = rcOutputRepository;
        this.webSocketHandler = webSocketHandler;
        this.relayControllerService = relayControllerService;
    }

    private String getIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Real-IP");
        if (ip == null)
            ip = request.getRemoteAddr();
        return ip;
    }

    private RCOutput findUserOutput(String sUuid, User user) {
        UUID uuid = null;
        try {
            uuid = UUID.fromString(sUuid);
        } catch (Exception ignored) {}
        if (uuid == null) {
            return null;
        }
        RCOutput rcOutput = rcOutputRepository.findById(uuid).orElse(null);
        if (rcOutput != null && rcOutput.getRelayController() != null &&
                user.equals(rcOutput.getRelayController().getUser())) {
            return rcOutput;
        }
        return null;
    }

    @GetMapping("")
    public void head(HttpServletRequest request) {
        // Проверка доступности Endpoint URL провайдера
        aliceRequestLogService.writeLog("head", getIp(request), "", "", "", "");
    }

    @PostMapping(path = "/user/unlink", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> unlink(HttpServletRequest request, @RequestHeader("X-Request-Id") String requestId) {
        aliceRequestLogService.writeLog("unlink", getIp(request), requestId,
                (String)request.getAttribute("username"), (String)request.getAttribute("token"), "");
        // POST	/v1.0/user/unlink	Оповещение о разъединении аккаунтов
        //-H 'Authorization: Bearer 123qwe456a...' \
        //-H 'X-Request-Id: ff36a3cc-ec...'
        // TODO : implement method
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("request_id", requestId);
        return ResponseEntity.ok().body(map);
    }

    @GetMapping(path = "/user/devices", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<?> getDevices(HttpServletRequest request,
                                        @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        WSCheck chk = processCheck(request, requestId,
                "/user/devices", null);
        if (chk.hasError()) {
            return chk.getResponseEntity();
        }
        User user = chk.getUser();

        // считаем все выходы контроллеров с признаком alice
        AliceDeviceResponse response = new AliceDeviceResponse();
        response.setRequestId(requestId);
        AlicePayload payload = new AlicePayload();
        payload.setUserId(user.getUsername());

        List<AliceDevice> aliceDevices = new ArrayList<>();
        List<RCOutput> outputs = rcOutputRepository.getAliceOutputs(user);
        for (RCOutput rcOutput: outputs) {
            aliceDevices.add(getAliceDevice(rcOutput));
        }
        payload.setDevices(aliceDevices);
        response.setPayload(payload);

        aliceRequestLogService.setResponse(chk.getLogId(), Utils.getJson(response));
        return ResponseEntity.ok().body(response);
    }

    private static AliceDevice getAliceDevice(RCOutput rcOutput) {
        AliceDevice aliceDevice = new AliceDevice();
        aliceDevice.setId(rcOutput.getUuid().toString());
        aliceDevice.setName(rcOutput.getName());
        aliceDevice.setDescription(rcOutput.getName());
        aliceDevice.setRoom(rcOutput.getRoom());
        aliceDevice.setType("devices.types.light"); // считаем все лампами

        AliceCapability aliceCapability = new AliceCapability();
        aliceCapability.setType("devices.capabilities.on_off");
        List<AliceCapability> aliceCapabilities = new ArrayList<>();
        aliceCapabilities.add(aliceCapability);
        aliceDevice.setAliceCapabilities(aliceCapabilities);
        return aliceDevice;
    }

    @PostMapping(path = "/user/devices/query", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<?> queryDevices(HttpServletRequest request,
                                          @RequestHeader(value = "X-Request-Id", required = false) String requestId,
                                          @RequestBody AliceDeviceStateRequest deviceStateRequest) {
        WSCheck chk = processCheck(request, requestId,
                "/user/devices/query", deviceStateRequest.toString());
        if (chk.hasError()) {
            return chk.getResponseEntity();
        }
        User user = chk.getUser();

        AliceDeviceStateResponse deviceStateResponse = new AliceDeviceStateResponse();
        deviceStateResponse.setRequest_id(requestId);

        AlicePayload alicePayload = new AlicePayload();
        List<AliceDevice> devices = new ArrayList<>();

        for (AliceDevice device : deviceStateRequest.getDevices()) {
            RCOutput rcOutput = null;
            try {
                rcOutput = findUserOutput(device.getId(), user);
                //rcOutput = rcOutputRepository.findById(UUID.fromString(device.getId())).orElse(null);
                if (rcOutput != null && (!rcOutput.getAlice() ||
                                         rcOutput.getRelayController() != null &&
                                         !rcOutput.getRelayController().getUser().equals(user))) { // security check
                    rcOutput = null; // if no alice flag do device not found
                }
            } catch (Exception ignored) {
            }
            AliceDevice aliceDevice = new AliceDevice();
            aliceDevice.setId(device.getId());
            if (rcOutput == null) {
                aliceDevice.setErrorCode("DEVICE_NOT_FOUND");
                aliceDevice.setErrorMessage("Device not found");
            } else {
                aliceDevice.setName(rcOutput.getName());
                aliceDevice.setDescription(rcOutput.getName());
                aliceDevice.setRoom(rcOutput.getRoom());
                aliceDevice.setType("devices.types.light"); // считаем все лампами

                AliceCapability aliceCapability = new AliceCapability();
                aliceCapability.setType("devices.capabilities.on_off");
                AliceState aliceState = new AliceState();
                aliceState.setInstance("on");
                aliceState.setValue("on".equalsIgnoreCase(rcOutput.getState()));
                aliceCapability.setState(aliceState);
                List<AliceCapability> aliceCapabilities = new ArrayList<>();
                aliceCapabilities.add(aliceCapability);
                aliceDevice.setAliceCapabilities(aliceCapabilities);
            }
            devices.add(aliceDevice);
        }

        alicePayload.setDevices(devices);
        deviceStateResponse.setPayload(alicePayload);

        aliceRequestLogService.setResponse(chk.getLogId(), Utils.getJson(deviceStateResponse));
        return ResponseEntity.ok().body(deviceStateResponse);
    }

    @PostMapping(path = "/user/devices/action", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<?> deviceAction(HttpServletRequest request,
                                          @RequestHeader(value = "X-Request-Id", required = false) String requestId,
                                          @RequestBody AliceDeviceActionRequest deviceActionRequest) {
        WSCheck chk = processCheck(request, requestId,
                "/user/devices/action", Utils.getJson(deviceActionRequest));
        if (chk.hasError()) {
            return chk.getResponseEntity();
        }
        User user = chk.getUser();

        AliceDeviceActionResponse aliceDeviceActionResponse = new AliceDeviceActionResponse();
        aliceDeviceActionResponse.setRequest_id(requestId);

        List<AliceDevice> aliceDevices = new ArrayList<>();
        for (AliceDevice aliceDevice : deviceActionRequest.getPayload().getDevices()) {
            RCOutput output = null;
            try {
                //output = rcOutputRepository.findById(UUID.fromString(aliceDevice.getId())).orElse(null);
                output = findUserOutput(aliceDevice.getId(), user);
                if (output != null && !output.getAlice()) {
                    output = null;
                }
            } catch (Exception ignore) { }
            AliceActionResult actionResult = new AliceActionResult();
            List<AliceCapability> aliceCapabilities = new ArrayList<>();
            if (output == null) {
                actionResult.setStatus("ERROR");
                actionResult.setErrorCode("DEVICE_NOT_FOUND");
            } else {
                // по каждому умению нужно отработать и предоставить ответ
                for (AliceCapability aliceCapability : aliceDevice.getAliceCapabilities()) {
                    AliceState aliceState = aliceCapability.getState();
                    if ("devices.capabilities.on_off".equals(aliceCapability.getType())) {
                        String value = "off";
                        if ((Boolean) aliceCapability.getState().getValue())
                            value = "on";
                        String mac = output.getRelayController().getMac();
                        String message = relayControllerService.getDeviceActionMessage(output, value);
//                        String res = webSocketHandler.sendMessageToController(mac, message);
//                        if ("OK".equals(res)) {
//                            actionResult.setStatus("DONE");
//                        } else if ("NOT_FOUND".equals(res) || "SESSION_CLOSED".equals(res)) {
//                            actionResult.setStatus("ERROR");
//                            actionResult.setErrorCode("DEVICE_UNREACHABLE");
//                        } else {
//                            actionResult.setStatus("ERROR");
//                            actionResult.setErrorCode("UNKNOWN_ERROR");
//                        }
                    }
                    aliceState.setActionResult(actionResult);
                    aliceCapability.setState(aliceState);
                    aliceCapabilities.add(aliceCapability);
                }
            }
            aliceDevice.setAliceCapabilities(aliceCapabilities);
            aliceDevices.add(aliceDevice);
        }
        AlicePayload alicePayload = new AlicePayload();
        alicePayload.setDevices(aliceDevices);
        aliceDeviceActionResponse.setPayload(alicePayload);

        aliceRequestLogService.setResponse(chk.getLogId(), Utils.getJson(aliceDeviceActionResponse));
        return ResponseEntity.ok().body(aliceDeviceActionResponse);
    }
}
