package kz.home.RelaySmartSystems.model;

import lombok.Getter;

@Getter
public enum EErrorCodes {
    E_NO_ERROR(0),
    E_ERROR(1),
    E_HELLO_ERROR(2),
    E_CRC_ERROR(3),
    E_NOMAC_ERROR(4),
    E_TOKEN_ERROR(5),
    E_NODE_ERROR(6),
    E_SERVER_ERROR(7),
    E_NOT_AUTHRORIZED(8);

    private final int value;

    EErrorCodes(int value) {
        this.value = value;
    }

}
