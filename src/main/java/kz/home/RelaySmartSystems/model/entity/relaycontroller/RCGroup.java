package kz.home.RelaySmartSystems.model.entity.relaycontroller;

import kz.home.RelaySmartSystems.Utils;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "rc_group")
public class RCGroup {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    private String name;
    @Transient
    private String gid;

    @OneToMany(mappedBy = "group")
    private List<RelayController> controllers = new ArrayList<>();

    public String getGid() {
        try {
            return Utils.getShortId(id.toString(), 6);
        } catch (Exception ignore) {
            return null;
        }
    }
}