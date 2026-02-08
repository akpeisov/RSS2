package kz.home.RelaySmartSystems.repository;

import kz.home.RelaySmartSystems.model.entity.User;
import kz.home.RelaySmartSystems.model.entity.relaycontroller.RCOutput;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
@Repository
public interface RCOutputRepository extends JpaRepository<RCOutput, UUID> {
    //@Query("select o from Output o where o.RelayController = ?1 and o.id = ?2")
    @Query(value = "select * from rc_outputs o where o.relay_controller_uuid = ?1 and o.id = ?2 and coalesce(o.slave_id, 0) = coalesce(?3, 0)", nativeQuery = true)
    RCOutput findOutput(UUID relayControllerId, Integer output, Integer slaveId);

    @Query("select o from RCOutput o, RelayController r where r.uuid = o.relayController and r.user = ?1 and o.alice = true")
    List<RCOutput> getAliceOutputs(User user);
}
