package kz.home.RelaySmartSystems.model.entity.relaycontroller;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.UUID;

@Setter
@Getter
@NoArgsConstructor
@Entity
@Table(name = "rc_acls")
public class RCAcl {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID uuid;
    String type;
    Integer id;
    String io;
    String state;
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "event_uuid", nullable=false)
    RCEvent event;
}
