package kz.home.RelaySmartSystems.model.alice;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class AliceResponseError {
    private String requestId;
    private String status = "error";
    private String message;

    public AliceResponseError(String requestId, String message) {
        this.requestId = requestId;
        this.message = message;
    }
}
