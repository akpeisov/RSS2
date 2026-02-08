package kz.home.RelaySmartSystems.repository;

import kz.home.RelaySmartSystems.model.alice.AliceRequestLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AliceRequestLogRepository extends JpaRepository<AliceRequestLog, UUID> {

}
