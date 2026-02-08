package kz.home.RelaySmartSystems.repository;

import kz.home.RelaySmartSystems.model.entity.relaycontroller.RCAcl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

public interface RCAclRepository extends JpaRepository<RCAcl, UUID> {
    @Query(value = "delete from rc_acls where uuid = ?1", nativeQuery = true)
    @Modifying
    @Transactional
    void deleteRCAcl(UUID id);
}