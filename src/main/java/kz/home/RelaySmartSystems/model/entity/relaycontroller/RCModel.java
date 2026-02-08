package kz.home.RelaySmartSystems.model.entity.relaycontroller;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RCModel {
    UNKNOWN(0,0,0),
    RCV1S(4, 4, 4),
    RCV1B(10, 16, 10),
    RCV2S(4, 6, 4),
    RCV2M(6, 8, 6),
    RCV2B(12, 16, 12);

    private final int outputs;
    private final int inputs;
    private final int buttons;

    public static RCModel fromInt(int index) {
        RCModel[] models = RCModel.values();
        if (index < 0 || index >= models.length) {
            return UNKNOWN; // или выбрасывать ошибку
        }
        return models[index];
    }
}