package kz.home.RelaySmartSystems.model.alice;

import com.fasterxml.jackson.annotation.JsonInclude;

public class AliceState {
    private String instance;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Object value;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private AliceActionResult actionResult;

    public String getInstance() {
        return instance;
    }

    public void setInstance(String instance) {
        this.instance = instance;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public AliceActionResult getActionResult() {
        return actionResult;
    }

    public void setActionResult(AliceActionResult actionResult) {
        this.actionResult = actionResult;
    }
}
