package kz.home.RelaySmartSystems.model.entity.relaycontroller;

import lombok.Getter;

@Getter
public enum RCInType {
    s("Switch"),
    b("Button"),
    i("InvertedSwitch");

    private final String value;

    RCInType(String value) {
        this.value = value;
    }

    // Helper method to get an enum constant from its string key
    public static RCInType fromCode(String value) {
        for (RCInType errorCode : RCInType.values()) {
            if (errorCode.value.equalsIgnoreCase(value)) {
                return errorCode;
            }
        }
        throw new IllegalArgumentException("No constant with code " + value + " found");
    }
}
