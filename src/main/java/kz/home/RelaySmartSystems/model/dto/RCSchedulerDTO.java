package kz.home.RelaySmartSystems.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Set;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RCSchedulerDTO {
    private boolean enabled;
    private List<RCTaskDTO> tasks;
    private String mac;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RCTaskDTO {
        private String name;
        private Integer grace;
        private Integer time;
        private boolean done;
        private boolean enabled;
        private Set<Integer> dow;
        private List<RCTaskActionDTO> actions;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RCTaskActionDTO {
        private String action;
        private Integer output;
        private String type;
        private Integer input;
    }
}
