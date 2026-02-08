package kz.home.RelaySmartSystems.repository;

import kz.home.RelaySmartSystems.model.entity.unicontroller.UniController;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UniControllerRepository extends JpaRepository<UniController, UUID> {
}
