package kz.home.RelaySmartSystems.repository;

import kz.home.RelaySmartSystems.model.entity.relaycontroller.RCAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

public interface RCActionRepository extends JpaRepository<RCAction, UUID> {
    @Query(value = "delete from rc_actions where uuid = ?1", nativeQuery = true)
    @Modifying
    @Transactional
    void deleteRCAction(UUID id);
}