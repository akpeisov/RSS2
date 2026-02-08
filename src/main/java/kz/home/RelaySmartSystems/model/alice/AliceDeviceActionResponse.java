package kz.home.RelaySmartSystems.model.alice;

public class AliceDeviceActionResponse {
    private String request_id;
    private AlicePayload payload;

    public String getRequest_id() {
        return request_id;
    }

    public void setRequest_id(String request_id) {
        this.request_id = request_id;
    }

    public AlicePayload getPayload() {
        return payload;
    }

    public void setPayload(AlicePayload payload) {
        this.payload = payload;
    }
}
