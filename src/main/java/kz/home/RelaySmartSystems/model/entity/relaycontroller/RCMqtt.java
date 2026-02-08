package kz.home.RelaySmartSystems.model.entity.relaycontroller;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "rc_mqtt")
public class RCMqtt {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID uuid;

    private boolean enabled;
    private String url;
    @OneToMany(mappedBy = "mqtt", cascade = CascadeType.ALL)
    private List<RCMqttTopic> topics;

    @OneToOne
    @JoinColumn(name = "controller_uuid", unique = true)
    private RelayController controller;
}
