package kz.home.RelaySmartSystems.model.entity.relaycontroller;

import lombok.Getter;

@Getter
public enum RCOutType {
    s("Simple"),
    t("Timed"),
    o("Oneshot");

    private final String value;

    RCOutType(String value) {
        this.value = value;
    }
}
