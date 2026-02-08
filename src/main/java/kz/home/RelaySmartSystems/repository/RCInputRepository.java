package kz.home.RelaySmartSystems.repository;

import kz.home.RelaySmartSystems.model.entity.relaycontroller.RCInput;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface RCInputRepository extends JpaRepository<RCInput, UUID> {
    @Query(value = "select * from rc_inputs i where i.relay_controller_uuid = ?1 and i.id = ?2 and coalesce(i.slave_id, 0) = coalesce(?3, 0)", nativeQuery = true)
    RCInput findInput(UUID relayControllerId, Integer input, Integer slaveId);
}
