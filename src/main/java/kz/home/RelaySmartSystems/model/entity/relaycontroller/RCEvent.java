package kz.home.RelaySmartSystems.model.entity.relaycontroller;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.List;
import java.util.UUID;

@Setter
@Getter
@Entity
@NoArgsConstructor
@Table(name = "rc_events",
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"input_uuid", "event"})
       })
public class RCEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID uuid;

    private String event;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @OrderBy("order ASC")
    private List<RCAction> actions;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<RCAcl> acls;

    @JoinColumn(name = "input_uuid", nullable=false)
    @ManyToOne(optional = false)
    private RCInput input;
}
