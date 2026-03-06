package kz.home.RelaySmartSystems.controller;

import kz.home.RelaySmartSystems.model.entity.Controller;
import kz.home.RelaySmartSystems.model.entity.relaycontroller.RelayController;
import kz.home.RelaySmartSystems.service.ControllerService;
import kz.home.RelaySmartSystems.service.RelayControllerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequestMapping(value = "/test")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class TestController {
    private final RelayControllerService relayControllerService;
    private final ControllerService controllerService;

    public TestController(RelayControllerService relayControllerService, ControllerService controllerService) {
        this.relayControllerService = relayControllerService;
        this.controllerService = controllerService;
    }

    @GetMapping("/bconfig")
    ResponseEntity<byte[]> testConfig(String mac) {
        Controller c = controllerService.findController(mac);
        if (c == null) {
            return ResponseEntity.notFound().build();
        }
//        if (c instanceof RelayController rc) {
//            byte[] data = relayControllerService.makeBConfig(rc);
//            return ResponseEntity.ok()
//                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
//                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"data.bin\"")
//                    .body(data);
//        }

        return ResponseEntity.notFound().build();

    }

    @GetMapping("/oom")
    public void triggerOom() {
        List<byte[]> list = new ArrayList<>();
        while (true) {
            list.add(new byte[10 * 1024 * 1024]);
        }
    }
}
