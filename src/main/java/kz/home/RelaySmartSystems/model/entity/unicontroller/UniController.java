package kz.home.RelaySmartSystems.model.entity.unicontroller;

import kz.home.RelaySmartSystems.model.entity.Controller;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.Table;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "uni_controllers")
public class UniController extends Controller {
    private double temperature;
}
