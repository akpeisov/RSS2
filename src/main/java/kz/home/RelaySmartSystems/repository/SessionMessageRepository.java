package kz.home.RelaySmartSystems.repository;

import kz.home.RelaySmartSystems.model.entity.SessionMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SessionMessageRepository extends JpaRepository<SessionMessage, UUID> {
}
