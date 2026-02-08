package kz.home.RelaySmartSystems.model.def;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Info {
    private Integer freeMemory;
    private String uptime;
    private Integer uptimeRaw;
    private String curdate;
    private String name;
    private String description;
    private String version;
    private Integer wifiRSSI;
    private String ethIP;
    private String wifiIP;
    private String mac;
    private String model;
    private String resetReason;
}
