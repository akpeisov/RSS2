package kz.home.RelaySmartSystems.model.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import kz.home.RelaySmartSystems.model.dto.*;
import kz.home.RelaySmartSystems.model.entity.relaycontroller.*;
import kz.home.RelaySmartSystems.service.DeviceConfigService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class RCConfigMapper {
    private final RelayControllerMapper relayControllerMapper;
    private final RCSchedulerMapper rcSchedulerMapper;
    private final RCMqttMapper rcMqttMapper;
    private final ObjectMapper mapper = new ObjectMapper();
    private final DeviceConfigService deviceConfigService;
    public RCConfigMapper(RelayControllerMapper relayControllerMapper,
                          RCSchedulerMapper rcSchedulerMapper,
                          RCMqttMapper rcMqttMapper, DeviceConfigService deviceConfigService) {
        this.relayControllerMapper = relayControllerMapper;
        this.rcSchedulerMapper = rcSchedulerMapper;
        this.rcMqttMapper = rcMqttMapper;
        this.deviceConfigService = deviceConfigService;
    }

    public RCConfigDTO RCtoDto(RelayController controller) {
        RCConfigDTO rcConfigDTO = new RCConfigDTO();
        // general info
        rcConfigDTO.setName(controller.getName());
        rcConfigDTO.setMac(controller.getMac());
        rcConfigDTO.setDescription(controller.getDescription());
        rcConfigDTO.setModel(controller.getModel().name());
        rcConfigDTO.setStatus(controller.getStatus());
        rcConfigDTO.setLastSeen(controller.getLastSeen());
        rcConfigDTO.setConfig(deviceConfigService.getConfig(controller));
        rcConfigDTO.setGroupID(controller.getGroup() != null ? controller.getGroup().getGid() : null);

        // io
        RCIOConfigDTO rcioConfigDTO = new RCIOConfigDTO();
        rcioConfigDTO.setOutputs(relayControllerMapper.outputsToDTO(controller.getOutputs()));

        // сначала получить inputs DTO
        List<RCInputDTO> inputs = relayControllerMapper.inputsToDTO(controller.getInputs());
        rcioConfigDTO.setInputs(inputs);

        rcConfigDTO.setIo(rcioConfigDTO);

        /*
        // modbus
        RCModbusConfigDTO rcModbusInfoDTO = relayControllerMapper.modbusToDTO(controller.getModbusConfig());
        // for master include all slaves
        if (rcModbusInfoDTO != null && "master".equalsIgnoreCase(rcModbusInfoDTO.getMode())) {
            List<RCModbusConfigDTO.SlaveDTO> slaveDTOS = new ArrayList<>();
            for (RCModbusConfig modbusConfig : modbusConfigRepository.findByMaster(controller.getMac())) {
                RCModbusConfigDTO.SlaveDTO slaveDTO = new RCModbusConfigDTO.SlaveDTO();
                slaveDTO.setMac(modbusConfig.getController().getMac());
                slaveDTO.setModel(modbusConfig.getController().getModel());
                slaveDTO.setSlaveId(modbusConfig.getSlaveId());
                slaveDTOS.add(slaveDTO);
            }
            rcModbusInfoDTO.setSlaves(slaveDTOS);
        }
        rcConfigDTO.setModbus(rcModbusInfoDTO);
        // network
        rcConfigDTO.setNetwork(networkToDto(controller.getNetworkConfig()));
        // scheduler
        rcConfigDTO.setScheduler(rcSchedulerMapper.toDto(controller.getScheduler()));
        // mqtt
        rcConfigDTO.setMqtt(rcMqttMapper.toDto(controller.getMqtt()));

        rcConfigDTO.setUsername(controller.getUser() != null ? controller.getUser().getUsername() : null);

    }

         */
        return rcConfigDTO;
    }
}
