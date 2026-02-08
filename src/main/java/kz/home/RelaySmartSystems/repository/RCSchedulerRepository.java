package kz.home.RelaySmartSystems.repository;

import kz.home.RelaySmartSystems.model.entity.relaycontroller.RCScheduler;
import kz.home.RelaySmartSystems.model.entity.relaycontroller.RelayController;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RCSchedulerRepository extends JpaRepository<RCScheduler, UUID> {
    RCScheduler findByController(RelayController controller);
}