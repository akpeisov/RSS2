package kz.home.RelaySmartSystems.model.entity;

import com.fasterxml.jackson.databind.JsonNode;
import kz.home.RelaySmartSystems.model.dto.JsonNodeConverter;
import kz.home.RelaySmartSystems.model.entity.relaycontroller.RelayController;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "device_configurations")
@Getter
@Setter
public class DeviceConfiguration {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID uuid;

    @ManyToOne(optional = false)
    @JoinColumn(name = "controller_uuid", nullable=false)
    private Controller controller;

    @Column(name = "config_data", columnDefinition = "jsonb")
    @Convert(converter = JsonNodeConverter.class)
    private JsonNode configData;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        //updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}