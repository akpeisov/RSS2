package kz.home.RelaySmartSystems.model.entity.relaycontroller;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "rc_mqtt_event")
public class RCMqttEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID uuid;

    private String event;
    private String type;
    private Integer output;
    private Integer input;
    private String action;
    private Integer slaveId;

    @ManyToOne
    @JoinColumn(name = "topic_uuid", nullable=false)
    private RCMqttTopic topic;
}
