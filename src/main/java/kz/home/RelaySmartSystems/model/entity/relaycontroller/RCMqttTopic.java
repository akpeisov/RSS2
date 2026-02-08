package kz.home.RelaySmartSystems.model.entity.relaycontroller;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "rc_mqtt_topic")
public class RCMqttTopic {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID uuid;

    private String topic;
    @OneToMany(mappedBy = "topic", cascade = CascadeType.ALL)
    private List<RCMqttEvent> events;

    @ManyToOne
    @JoinColumn(name = "mqtt_uuid", nullable=false)
    private RCMqtt mqtt;
}
