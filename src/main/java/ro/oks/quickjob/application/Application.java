package ro.oks.quickjob.application;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;
import ro.oks.quickjob.common.error.BusinessException;
import ro.oks.quickjob.common.error.ErrorCode;

@Entity
@Table(name = "applications")
@Getter
@NoArgsConstructor
public class Application {

    @Id
    private UUID id;

    @Column(name = "mission_id", nullable = false)
    private UUID missionId;

    @Column(name = "worker_id", nullable = false)
    private UUID workerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ApplicationStatus status;

    @Column(name = "message", length = 500)
    private String message;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public Application(UUID id, UUID missionId, UUID workerId, String message) {
        this.id = id;
        this.missionId = missionId;
        this.workerId = workerId;
        this.message = message;
        this.status = ApplicationStatus.PENDING;
    }

    public void accept() {
        requireStatus(ApplicationStatus.PENDING, "accepter");
        this.status = ApplicationStatus.ACCEPTED;
    }

    public void reject() {
        requireStatus(ApplicationStatus.PENDING, "rejeter");
        this.status = ApplicationStatus.REJECTED;
    }

    public void cancel() {
        requireStatus(ApplicationStatus.PENDING, "annuler");
        this.status = ApplicationStatus.CANCELLED;
    }

    private void requireStatus(ApplicationStatus expected, String action) {
        if (this.status != expected) {
            throw new BusinessException(
                    ErrorCode.APPLICATION_INVALID_TRANSITION,
                    "application.invalid_transition",
                    HttpStatus.CONFLICT, action, this.status.name());
        }
    }

    @PrePersist
    void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}
