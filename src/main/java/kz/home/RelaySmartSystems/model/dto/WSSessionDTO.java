package kz.home.RelaySmartSystems.model.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
public class WSSessionDTO {
    private String controllerId;
    private String type;
    private String username;
    private LocalDateTime connectionDate;
    private String clientIP;

}
