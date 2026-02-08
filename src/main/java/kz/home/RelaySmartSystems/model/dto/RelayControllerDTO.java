package kz.home.RelaySmartSystems.model.dto;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RelayControllerDTO {
    private UUID uuid;
    private String name;
    private String mac;
    private String type;
    private String status;
    private String description;
    private String model;
    private List<RCOutputDTO> outputs;
    private List<RCInputDTO> inputs;



    // TODO : network config, scheduler, mqtt etc.
}