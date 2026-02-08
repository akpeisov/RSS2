package kz.home.RelaySmartSystems.model.entity;

import com.fasterxml.jackson.annotation.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.util.Date;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "controllers")
@EntityListeners(AuditingEntityListener.class)
@Inheritance(strategy = InheritanceType.JOINED)
public class Controller {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID uuid;
    private String mac;
    @JsonIgnore
    @ManyToOne
    private User user;
    private Date firstDate;
    private String type;
    private Date linkDate;
    private String name;
    private String description;
    private String status;
    private Integer uptimeRaw;
    private String uptime;
    private Integer freeMemory;
    private String version;
    private String ethIP;
    private String wifiIP;
    private Integer wifiRSSI;
    private Long crc;
    private String model;
    private String resetReason;
    @Column(columnDefinition = "text")
    private String hwParams;
    @LastModifiedDate
    private Date lastSeen;

    @PrePersist
    void firstDate() {
        this.firstDate = new Date();
    }

    public boolean isLinked() {
        return user != null;
    }
}
