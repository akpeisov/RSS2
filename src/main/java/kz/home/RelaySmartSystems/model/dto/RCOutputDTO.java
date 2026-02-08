package kz.home.RelaySmartSystems.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class RCOutputDTO {
    private UUID uuid;
    private Integer id;
    private String name;
    private Long limit;
    private Long timer;
    private String type;
    @JsonProperty("default")
    private String _default;
    private String state;
    private Boolean alice;
    private String room;
    private Integer on;
    private Integer off;

    public RCOutputDTO(UUID uuid, Integer id, String name, Long limit, String type, String _default,
                       String state, Boolean alice, String room, Integer on, Integer off) {
        this.uuid = uuid;
        this.id = id;
        this.name = name;
        this.limit = limit;
        this.type = type == null ? "Simple" : type;
        this._default = _default;
        this.state = state;
        this.alice = alice;
        this.room = room;
        this.on = on;
        this.off = off;
    }
}