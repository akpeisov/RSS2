package kz.home.RelaySmartSystems.model.alice;

import com.fasterxml.jackson.annotation.*;

import java.util.List;
@JsonIgnoreProperties(ignoreUnknown = true)
public class AliceDevice {
    private String id;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String name;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String description;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String type;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String room;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String errorCode;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String errorMessage;
    @JsonProperty("capabilities")
    private List<AliceCapability> aliceCapabilities;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }

    public List<AliceCapability> getAliceCapabilities() {
        return aliceCapabilities;
    }

    public void setAliceCapabilities(List<AliceCapability> aliceCapabilities) {
        this.aliceCapabilities = aliceCapabilities;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}