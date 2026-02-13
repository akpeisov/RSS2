package kz.home.RelaySmartSystems.model.def;

import kz.home.RelaySmartSystems.model.entity.CModel;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class DeviceInfo {
    private String mac;
    private long freeMemory;
    private long uptimeRaw;
    private int version;
    private long curdate;
    private int wifiRSSI;
    private String ethIP;
    private String wifiIP;
    private String resetReason;
}