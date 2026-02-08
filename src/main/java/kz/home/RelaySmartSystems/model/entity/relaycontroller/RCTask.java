package kz.home.RelaySmartSystems.model.entity.relaycontroller;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.*;

@Getter
@Setter
@Entity
@Table(name = "rc_tasks")
public class RCTask {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID uuid;

    private String name;
    private Integer grace;
    private Integer time;
    private boolean done;
    private boolean enabled;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "rc_task_dow", joinColumns = @JoinColumn(name = "task_id"))
    @Column(name = "day_of_week")
    private Set<Integer> dow;

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<RCTaskAction> actions = new ArrayList<>();

    @ManyToOne(optional = false)
    @JoinColumn(name = "scheduler_uuid", nullable=false)
    private RCScheduler scheduler;
}
