package kz.home.RelaySmartSystems.model.alice;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.Date;
import java.util.UUID;

@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(name = "alice_request_log")
public class AliceRequestLog {
    @Id
    @GeneratedValue
    private UUID uuid;
    private Date datetime;
    @Column(length = 100)
    private String method;
    private String requestId;
    @Column(length = 2000)
    private String token;
    private String username;
    @Lob
    private String request;
    @Lob
    private String response;
    @Column(length = 20)
    private String sourceIP;

    @PrePersist
    void datetime() {
        this.datetime = new Date();
    }
}
