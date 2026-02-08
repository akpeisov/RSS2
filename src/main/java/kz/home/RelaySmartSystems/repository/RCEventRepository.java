package kz.home.RelaySmartSystems.repository;

import kz.home.RelaySmartSystems.model.entity.relaycontroller.RCEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

public interface RCEventRepository extends JpaRepository<RCEvent, UUID> {
    @Query(value = "delete from rc_events where uuid = ?1", nativeQuery = true)
    @Modifying
    @Transactional
    void deleteEvent(UUID id);
}
