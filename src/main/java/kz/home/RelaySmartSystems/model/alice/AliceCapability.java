package kz.home.RelaySmartSystems.model.alice;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
public class AliceCapability {
    private String type;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private AliceState state;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String parameters;
}
