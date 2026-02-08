package kz.home.RelaySmartSystems.model.mapper;

import kz.home.RelaySmartSystems.model.entity.relaycontroller.RCInType;
import kz.home.RelaySmartSystems.model.entity.relaycontroller.RCOutType;

public class BConfigMapper {
    public static byte mapOutputType(RCOutType t) {
        return switch (t) {
            case s -> 1;
            case t -> 2;
            case o -> 3;
            default -> throw new IllegalArgumentException("Bad output type " + t);
        };
    }

    public static byte mapInputType(RCInType t) {
        return switch (t) {
            case b -> 1;
            case s -> 2;
            case i -> 3;
            default -> 1;
        };
    }

    public static byte mapEvent(String e) {
        return switch (e) {
            case "ON" -> 1;
            case "OFF" -> 2;
            case "TOGGLE" -> 3;
            case "LONG" -> 4;
            default -> 5;
        };
    }

    public static byte mapAction(String a) {
        return switch (a.toUpperCase()) {
            case "ON" -> 1;
            case "OFF" -> 2;
            case "TOGGLE" -> 3;
            case "WAIT" -> 4;
            case "ALLOFF" -> 5;
            default -> 1;
        };
    }
}
