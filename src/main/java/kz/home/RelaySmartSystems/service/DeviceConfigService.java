package kz.home.RelaySmartSystems.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import kz.home.RelaySmartSystems.model.entity.Controller;
import kz.home.RelaySmartSystems.model.entity.DeviceConfiguration;
import kz.home.RelaySmartSystems.repository.DeviceConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceConfigService {

    private final DeviceConfigRepository repo;

    public JsonNode getConfig(Controller controller) {
        return repo.findByController(controller)
                .map(DeviceConfiguration::getConfigData)
                .orElse(null);
    }

    public void saveConfig(JsonNode json, Controller controller) {
        DeviceConfiguration entity = repo.findByController(controller)
                .orElse(new DeviceConfiguration());
        entity.setConfigData(json);
        repo.save(entity);
    }
}



/*
* @RestController
@RequestMapping("/api/config")
@RequiredArgsConstructor
public class DeviceConfigController {

    private final DeviceConfigService configService;

    @PostMapping
    public ResponseEntity<DeviceConfigDTO> saveConfig(@RequestBody String jsonConfig)
            throws JsonProcessingException {
        DeviceConfigDTO saved = configService.saveConfig(jsonConfig);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/{mac}")
    public ResponseEntity<DeviceConfigDTO> getConfig(@PathVariable String mac) {
        DeviceConfigDTO config = configService.getConfigByMac(mac);
        return ResponseEntity.ok(config);
    }

    @GetMapping("/{mac}/json")
    public ResponseEntity<String> getConfigAsJson(@PathVariable String mac)
            throws JsonProcessingException {
        String json = configService.getConfigAsJsonString(mac);
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(json);
    }

    @PatchMapping("/{mac}")
    public ResponseEntity<DeviceConfigDTO> updateConfig(
            @PathVariable String mac,
            @RequestBody String jsonPatch) throws JsonProcessingException {

        DeviceConfigDTO updated = configService.updatePartialConfig(mac, jsonPatch);
        return ResponseEntity.ok(updated);
    }
}
* */