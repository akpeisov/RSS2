package kz.home.RelaySmartSystems.model.def;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class CResponse {
    private String message;

    public CResponse(String message) {
        this.message = message;
    }

}
