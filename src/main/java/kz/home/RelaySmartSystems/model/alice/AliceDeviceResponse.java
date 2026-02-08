package kz.home.RelaySmartSystems.model.alice;

public class AliceDeviceResponse {
    private String requestId;
    private AlicePayload payload;

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public AlicePayload getPayload() {
        return payload;
    }

    public void setPayload(AlicePayload payload) {
        this.payload = payload;
    }
}