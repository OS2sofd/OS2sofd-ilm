package dk.digitalidentity.OS2ilm.model.entity;

import dk.digitalidentity.OS2ilm.model.entity.enums.LogAction;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity(name = "auditlog")
public class AuditLog {

    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @CreationTimestamp
    @Column
    private LocalDateTime tts;

    @Column
    private String ipAddress;

    @Column
    private String location;

    @Column
    private String correlationId;

    @Column
    private Long targetId;

    @Column
    private String targetName;

    // referenced entity that performed the action (null if the performer was the data-owner)
    @Column
    private String performerId;

    @Column
    private String performerName;

    // action performed
    @Column
    @Enumerated(EnumType.STRING)
    private LogAction logAction;

    @Column
    private String message;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JoinColumn(name = "auditlogs_details_id")
    private AuditLogDetail details;
}
