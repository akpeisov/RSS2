package kz.home.RelaySmartSystems.controller;

import kz.home.RelaySmartSystems.model.entity.User;
import kz.home.RelaySmartSystems.service.RelayControllerService;
import kz.home.RelaySmartSystems.service.UserService;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping(value = "/webapi")
//@CrossOrigin(origins = "http://localhost")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class WebAPI {
    private static final Logger logger = LoggerFactory.getLogger(WebAPI.class);
    private final UserService userService;
    @Value("${env}")
    private String env;
    @Value("${keycloak.secret}")
    String keycloakSecret;
    public WebAPI(UserService userService
                  ) {
        this.userService = userService;
    }

    @Data
    public static class KeycloakEvent {
        private String type;
        private String realmId;
        private String clientId;
        private String userId;
        private Map<String, String> details;
    }

    @PostMapping("/keycloak/events")
    public ResponseEntity<Void> onEvent(@RequestBody KeycloakEvent event ,
                                        @RequestHeader("x-webhook-secret") String secret) {
        // т.к. это webhook, нет смысла возвращать ошибки
        if (secret == null) {
            logger.error("secret header not defined");
            return ResponseEntity.badRequest().build();
        }

        if (keycloakSecret == null) {
            logger.error("secret not defined");
            return ResponseEntity.internalServerError().build();
        }

        if (!secret.equals(keycloakSecret)) {
            logger.error("secret not equals, {}", secret);
            return ResponseEntity.status(403).build();
        }

        String eventType = event.getType();
        switch (eventType) {
            case "REGISTER":
                logger.info("Received Keycloak registration event for userId: {}", event.getUserId());
                handleRegistration(event);
                break;
            case "UPDATE_PROFILE":
                logger.info("Received Keycloak profile update event for userId: {}", event.getUserId());
                handleUpdate(event);
                break;
            case "DELETE_ACCOUNT":
                logger.info("Received Keycloak account deletion event for userId: {}", event.getUserId());
                break;
            default:
                logger.info("Received Keycloak event of type: {} for userId: {}", eventType, event.getUserId());
        }
        return ResponseEntity.ok().build();
    }

    private void handleRegistration(KeycloakEvent event) {
        String username = event.getDetails().get("username");
        String email = event.getDetails().get("email");
        UUID userId = UUID.fromString(event.getUserId());
        String firstname = event.getDetails().get("firstname");
        String lastname = event.getDetails().get("lastname");
        if (userService.findByUsername(username) != null) {
            logger.error("User with username {} already exists. Skipping creation.", username);
            return;
        }
        userService.addUser(userId, username, firstname, lastname, email);
    }

    private void handleUpdate(KeycloakEvent event) {
        String username = event.getDetails().get("username");
        String email = event.getDetails().get("email");
        UUID userId = UUID.fromString(event.getUserId());
        String firstname = event.getDetails().get("firstname");
        String lastname = event.getDetails().get("lastname");
        User user = userService.findByUuid(userId);
        if (user != null) {
            user.setUsername(username);
            user.setFirstName(firstname);
            user.setLastName(lastname);
            user.setEmail(email);
            userService.saveUser(user);
        }
    }
}
