package kz.home.RelaySmartSystems.model.entity.relaycontroller;

import kz.home.RelaySmartSystems.Utils;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Setter
@Getter
@Entity
@Table(name = "rc_inputs")
public class RCInput {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID uuid;
    @Column(nullable = false)
    private Integer id;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false)
    private RCInType type;
    //private String type;
    private String state;
    @OneToMany(mappedBy = "input", orphanRemoval = true, cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<RCEvent> events;
    @JoinColumn(name = "relay_controller_uuid", nullable=false)
    @ManyToOne(optional = false)
    private RelayController relayController;

    public Long getCRC() {
        StringBuilder sb = new StringBuilder();

        if (this.getEvents() != null) {
            this.getEvents().stream()
                    .sorted(Comparator.comparing(RCEvent::getEvent)) // сортировка по имени события
                    .forEach(event -> {
                        sb.append(";event=").append(event.getEvent());

                        // actions
                        if (event.getActions() != null) {
                            event.getActions().stream()
                                    .sorted(Comparator.comparing(RCAction::getOrder))
                                    .forEach(action -> {
                                        sb.append(";action[")
                                                .append("order=").append(action.getOrder())
                                                .append(",act=").append(action.getAction())
                                                .append(",dur=").append(action.getDuration())
                                                .append(",output=").append(action.getOutput().getId())
                                                .append("]");
                                    });
                        }

                        // acls
                        if (event.getAcls() != null) {
                            event.getAcls().stream()
                                    .sorted(Comparator.comparing(RCAcl::getId, Comparator.nullsFirst(Integer::compareTo)))
                                    .forEach(acl -> {
                                        sb.append(";acl[")
                                                .append("type=").append(acl.getType())
                                                .append(",id=").append(acl.getId())
                                                .append(",io=").append(acl.getIo())
                                                .append(",state=").append(acl.getState())
                                                .append("]");
                                    });
                        }
                    });
        }
        return Utils.getCRC(sb.toString());
    }
}
