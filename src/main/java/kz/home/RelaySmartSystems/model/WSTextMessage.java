package kz.home.RelaySmartSystems.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Getter
@Setter
@NoArgsConstructor
public class WSTextMessage {
    private String type;
    //@JsonBackReference
    private Object payload;

    private static final Logger logger = LoggerFactory.getLogger(WSTextMessage.class);

    public WSTextMessage(String type, Object payload) {
        this.type = type;
        this.payload = payload;
    }

    public String makeMessage() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            logger.error(e.getLocalizedMessage());
            return null;
        }
    }

    public static String send(String type, Object payload) {
        return new WSTextMessage(type, payload).makeMessage();
    }
}
