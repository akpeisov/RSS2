package kz.home.RelaySmartSystems.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.util.Date;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RCConfigDTO {
    private String mac;
    private String name;
    private String description;
    private String model;
    private String status;
    private RCIOConfigDTO io;
    private String username;
    private Date lastSeen;
    private JsonNode config;
    private String groupID;
    //private Map<String, Object> hwParams;
}