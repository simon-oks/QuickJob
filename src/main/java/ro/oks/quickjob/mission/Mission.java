package ro.oks.quickjob.mission;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Point;
import org.springframework.http.HttpStatus;
import ro.oks.quickjob.common.error.BusinessException;
import ro.oks.quickjob.common.error.ErrorCode;

@Entity
@Table(name = "missions")
@Getter
@NoArgsConstructor
public class Mission {

    @Id
    private UUID id;

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    private MissionCategory category;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "location", nullable = false, columnDefinition = "geography(Point,4326)")
    private Point location;

    @Column(name = "address_label", nullable = false, length = 255)
    private String addressLabel;

    @Column(name = "start_at", nullable = false)
    private OffsetDateTime startAt;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Column(name = "amount_minor", nullable = false)
    private long amountMinor;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "required_workers", nullable = false)
    private int requiredWorkers;

    @ElementCollection
    @CollectionTable(name = "mission_workers", joinColumns = @JoinColumn(name = "mission_id"))
    @Column(name = "worker_id")
    private Set<UUID> workerIds = new HashSet<>();

    @lombok.Setter
    @Column(name = "notification_radius_meters")
    private Integer notificationRadiusMeters;

    @lombok.Setter
    @Column(name = "delivery_location", columnDefinition = "geography(Point,4326)")
    private Point deliveryLocation;

    @lombok.Setter
    @Column(name = "delivery_address_label", length = 255)
    private String deliveryAddressLabel;

    @lombok.Setter
    @Column(name = "additional_info", length = 2000)
    private String additionalInfo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MissionStatus status;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public Mission(UUID id, UUID clientId, MissionCategory category, String title,
                   String description, Point location, String addressLabel,
                   OffsetDateTime startAt, int durationMinutes,
                   long amountMinor, String currency, int requiredWorkers) {
        this.id = id;
        this.clientId = clientId;
        this.category = category;
        this.title = title;
        this.description = description;
        this.location = location;
        this.addressLabel = addressLabel;
        this.startAt = startAt;
        this.durationMinutes = durationMinutes;
        this.amountMinor = amountMinor;
        this.currency = currency;
        this.requiredWorkers = requiredWorkers;
        this.workerIds = new HashSet<>();
        this.status = MissionStatus.PUBLISHED;
    }

    public void assign(UUID workerId) {
        requireStatus(MissionStatus.PUBLISHED, "attribuer");

        if (workerIds.contains(workerId)) {
            throw new BusinessException(
                    ErrorCode.MISSION_WORKER_ALREADY_ASSIGNED,
                    "mission.worker_already_assigned",
                    HttpStatus.CONFLICT);
        }

        if (workerIds.size() >= requiredWorkers) {
            throw new BusinessException(
                    ErrorCode.MISSION_FULLY_ASSIGNED,
                    "mission.fully_assigned",
                    HttpStatus.CONFLICT);
        }

        workerIds.add(workerId);

        if (workerIds.size() == requiredWorkers) {
            this.status = MissionStatus.ASSIGNED;
        }
    }

    public boolean isFullyAssigned() {
        return workerIds.size() >= requiredWorkers;
    }

    public boolean hasWorker(UUID workerId) {
        return workerIds.contains(workerId);
    }

    public int getAssignedWorkersCount() {
        return workerIds.size();
    }

    public void start() {
        requireStatus(MissionStatus.ASSIGNED, "démarrer");
        this.status = MissionStatus.IN_PROGRESS;
    }

    public void complete() {
        requireStatus(MissionStatus.IN_PROGRESS, "terminer");
        this.status = MissionStatus.COMPLETED;
        this.completedAt = OffsetDateTime.now();
    }

    public void cancel() {
        if (status == MissionStatus.COMPLETED || status == MissionStatus.EXPIRED) {
            throw new BusinessException(
                    ErrorCode.MISSION_INVALID_TRANSITION,
                    "mission.cancel_invalid",
                    HttpStatus.CONFLICT, status.name().toLowerCase());
        }
        this.status = MissionStatus.CANCELLED;
    }

    public void expire() {
        requireStatus(MissionStatus.PUBLISHED, "expirer");
        this.status = MissionStatus.EXPIRED;
    }

    private void requireStatus(MissionStatus expected, String action) {
        if (this.status != expected) {
            throw new BusinessException(
                    ErrorCode.MISSION_INVALID_TRANSITION,
                    "mission.invalid_transition",
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
