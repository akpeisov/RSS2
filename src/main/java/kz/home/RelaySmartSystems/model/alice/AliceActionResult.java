package kz.home.RelaySmartSystems.model.alice;

import com.fasterxml.jackson.annotation.JsonInclude;

public class AliceActionResult {
    private String status;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String errorCode;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String errorMessage;

    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }
    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
