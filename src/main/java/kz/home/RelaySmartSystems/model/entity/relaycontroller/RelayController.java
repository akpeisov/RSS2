package kz.home.RelaySmartSystems.model.entity.relaycontroller;

import kz.home.RelaySmartSystems.model.entity.Controller;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "rc_controllers")
public class RelayController extends Controller {
    // mappedBy - имя "колонки" (точнее поля) в дочерней таблице, по которой будет связка с id данной

    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private RCGroup group;

    @OneToMany(mappedBy = "relayController", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<RCOutput> outputs;

    @OneToMany(mappedBy = "relayController", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<RCInput> inputs;

    @OneToOne(mappedBy = "controller", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private RCScheduler scheduler;

    @OneToOne(mappedBy = "controller", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private RCMqtt mqtt;
}
