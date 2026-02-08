package kz.home.RelaySmartSystems.model;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class TokenData {
    private String username;
    private String mac;
    private String model;
    private String sessionId;
    private String errorText;
    private Exception exception;
}
