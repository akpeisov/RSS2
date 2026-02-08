package kz.home.RelaySmartSystems.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RCUpdateIODTO {
    private String mac;
    private List<RCState> outputs;
    private List<RCState> inputs;

    @Data
    public static class RCState {
        private Integer id;
        private String state;
        private Integer slaveId = 0;
    }
}
