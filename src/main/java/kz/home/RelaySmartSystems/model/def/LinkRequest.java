package kz.home.RelaySmartSystems.model.def;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class LinkRequest {
    private String mac;
    private String event;
}
