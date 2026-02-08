package kz.home.RelaySmartSystems.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class AlertMessage {
    private static final String TYPE = "ALERT";

    private final Payload payload;

    private AlertMessage(String message) {
        this.payload = new Payload(message);
    }

    public static String makeAlert(String message) {
        AlertMessage error = new AlertMessage(message);
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(error);
        } catch (JsonProcessingException e) {
            return String.format("{\"type\":\"ALERT\",\"payload\":{\"message\":\"%s\"}}", message);
        }
    }

    @JsonProperty("type")
    public String getType() {
        return TYPE;
    }

    @JsonProperty("payload")
    public Payload getPayload() {
        return payload;
    }

    private static class Payload {
        private final String message;

        private Payload(String message) {
            this.message = message;
        }

        @JsonProperty("message")
        public String getMessage() {
            return message;
        }
    }
}
