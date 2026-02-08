package kz.home.RelaySmartSystems.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RCAclDTO {
    private UUID uuid;
    String type;
    Integer id;
    String io;
    String state;
}
