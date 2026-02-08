package kz.home.RelaySmartSystems.model.entity.relaycontroller;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "rc_scheduler")
public class RCScheduler {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID uuid;

    private boolean enabled;

    @OneToOne
    @JoinColumn(name = "relay_controller_uuid", unique = true)
    private RelayController controller;

    @OneToMany(mappedBy = "scheduler", cascade = CascadeType.ALL)
    private List<RCTask> tasks;
}
