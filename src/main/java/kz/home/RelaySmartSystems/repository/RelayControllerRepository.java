package kz.home.RelaySmartSystems.repository;

import kz.home.RelaySmartSystems.model.entity.User;
import kz.home.RelaySmartSystems.model.entity.relaycontroller.RelayController;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RelayControllerRepository extends JpaRepository<RelayController, UUID> {
    //List<RelayController> findByUser(User user);
//    RelayController findByMacIgnoreCase(String mac); // IgnoreCase якобы делает все сам, но и все вызовы должны быть соответствующие
    RelayController findByMac(String mac);
}
