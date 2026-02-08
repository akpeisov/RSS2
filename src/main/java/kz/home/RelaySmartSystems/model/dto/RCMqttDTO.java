package kz.home.RelaySmartSystems.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RCMqttDTO {
    private boolean enabled;
    private String url;
    private List<RCMqttTopicDTO> topics;
    private String mac;

    @Data
    public static class RCMqttTopicDTO {
        private String topic;
        private List<RCMqttEventDTO> events;
    }

    @Data
    public static class RCMqttEventDTO {
        private String event;
        private String type;
        private Integer output;
        private Integer input;
        private String action;
        private Integer slaveId;
    }
}