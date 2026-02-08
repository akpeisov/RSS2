package kz.home.RelaySmartSystems.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ErrorMessage {

    private static final String TYPE = "ERROR";

    private final Payload payload;

    private ErrorMessage(String message) {
        this.payload = new Payload(message);
    }

    public static String makeError(String message) {
        ErrorMessage error = new ErrorMessage(message);
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(error);
        } catch (JsonProcessingException e) {
            return String.format("{\"type\":\"ERROR\",\"payload\":{\"message\":\"%s\"}}", message);
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