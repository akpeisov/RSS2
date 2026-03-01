package kz.home.RelaySmartSystems.model.entity.relaycontroller;

import kz.home.RelaySmartSystems.model.def.DeviceInfo;
import kz.home.RelaySmartSystems.model.entity.CModel;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class RCDeviceInfo extends DeviceInfo {
    private int neighborCount;
    private List<NeighborInfo> neighbors = new ArrayList<>();

    private int outputStates;
    private int inputStates;

    @Getter
    @Setter
    public static class NeighborInfo {
        private String mac;
        private CModel model;
        private int outputsStates;
        private int inputsStates;
        private boolean isOnline;
    }
}
