package kz.home.RelaySmartSystems.model.entity.relaycontroller;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.UUID;

@Setter
@Getter
@NoArgsConstructor
@Entity
@Table(name = "rc_actions")
public class RCAction {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID uuid;
    @NotNull
    private Integer order;
    @JoinColumn(name = "output_uuid", nullable=false)
    @ManyToOne(optional = false)
    private RCOutput output;
    private String action;
    private Integer duration; // только для action = wait
    @JoinColumn(name = "target_rc_uuid")
    @ManyToOne
    private RelayController node;
    @JoinColumn(name = "event_uuid", nullable=false)
    @ManyToOne(optional = false)
    private RCEvent event;
}
