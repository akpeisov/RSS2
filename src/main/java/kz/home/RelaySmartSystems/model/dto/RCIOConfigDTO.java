package kz.home.RelaySmartSystems.model.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Set;

@Getter
@Setter
public class RCIOConfigDTO {
    private List<RCOutputDTO> outputs;
    private List<RCInputDTO> inputs;
}