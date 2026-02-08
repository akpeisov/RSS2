package kz.home.RelaySmartSystems.model.alice;

import java.util.List;

public class AliceDeviceStateRequest {
    private List<AliceDevice> devices;

    public List<AliceDevice> getDevices() {
        return devices;
    }

    public void setDevices(List<AliceDevice> devices) {
        this.devices = devices;
    }
}