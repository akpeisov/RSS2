package kz.home.RelaySmartSystems.model.alice;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

@Data
public class AlicePayload {
    private List<AliceDevice> devices;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String userId;
}
