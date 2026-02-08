package kz.home.RelaySmartSystems.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import kz.home.RelaySmartSystems.Utils;
import kz.home.RelaySmartSystems.model.entity.relaycontroller.RCAcl;
import kz.home.RelaySmartSystems.model.entity.relaycontroller.RCAction;
import kz.home.RelaySmartSystems.model.entity.relaycontroller.RCEvent;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class RCInputDTO {
    private UUID uuid;
    private Integer id;
    private String name;
    private String type;
    private String state;

    private List<RCEventDTO> events;

    public RCInputDTO(UUID uuid, Integer id, String name, String type, String state, List<RCEventDTO> events) {
        this.uuid = uuid;
        this.id = id;
        this.name = name;
        this.type = type == null ? "Switch" : type;
        if (this.id > 15)
            this.type = "Button";
        this.state = state;
        this.events = events;
    }

    public Long getCRC() {
        StringBuilder sb = new StringBuilder();

        if (this.getEvents() != null) {
            this.getEvents().stream()
                    .sorted(Comparator.comparing(RCEventDTO::getEvent)) // сортировка по имени события
                    .forEach(event -> {
                        sb.append(";event=").append(event.getEvent());

                        // actions
                        if (event.getActions() != null) {
                            event.getActions().stream()
                                    .sorted(Comparator.comparing(RCActionDTO::getOrder))
                                    .forEach(action -> {
                                        sb.append(";action[")
                                                .append("order=").append(action.getOrder())
                                                .append(",output=").append(action.getOutput())
                                                .append(",act=").append(action.getAction())
                                                .append(",dur=").append(action.getDuration())
                                                .append("]");
                                    });
                        }

                        // acls
                        if (event.getAcls() != null) {
                            event.getAcls().stream()
                                    .sorted(Comparator.comparing(RCAclDTO::getId, Comparator.nullsFirst(Integer::compareTo)))
                                    .forEach(acl -> {
                                        sb.append(";acl[")
                                                .append("type=").append(acl.getType())
                                                .append(",id=").append(acl.getId())
                                                .append(",io=").append(acl.getIo())
                                                .append(",state=").append(acl.getState())
                                                .append("]");
                                    });
                        }
                    });
        }
        return Utils.getCRC(sb.toString());
    }
}
