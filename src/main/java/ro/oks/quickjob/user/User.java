package ro.oks.quickjob.user;

import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.locationtech.jts.geom.Point;
import ro.oks.quickjob.mission.MissionCategory;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor
public class User {

    @Id
    private UUID id;

    @Column(name = "phone_number", nullable = false, unique = true, length = 20)
    @Setter
    private String phoneNumber;

    @Column(name = "display_name", nullable = false, length = 100)
    @Setter
    private String displayName;

    @Column(name = "roles", nullable = false, length = 50)
    private String roles;

    @Column(name = "phone_verified", nullable = false)
    @Setter
    private boolean phoneVerified;

    @Column(name = "preferred_language", nullable = false, length = 5)
    @Setter
    private String preferredLanguage = "fr";

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "last_known_location", columnDefinition = "geography(Point, 4326)")
    private Point lastKnownLocation;

    @Column(name = "last_location_at")
    private OffsetDateTime lastLocationAt;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "worker_categories", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "category")
    @Enumerated(EnumType.STRING)
    private Set<MissionCategory> workerCategories = new HashSet<>();

    public User(UUID id, String phoneNumber, String displayName) {
        this.id = id;
        this.phoneNumber = phoneNumber;
        this.displayName = displayName;
        this.roles = "CLIENT,WORKER";
    }

    public Set<Role> getRoleSet() {
        Set<Role> set = EnumSet.noneOf(Role.class);
        if (roles != null) {
            for (String r : roles.split(",")) {
                set.add(Role.valueOf(r.trim()));
            }
        }
        return set;
    }

    public void setRoleSet(Set<Role> roleSet) {
        this.roles = roleSet.stream()
                .map(Enum::name)
                .sorted()
                .reduce((a, b) -> a + "," + b)
                .orElse("");
    }

    public boolean hasRole(Role role) {
        return getRoleSet().contains(role);
    }

    public void updateLocation(Point location) {
        this.lastKnownLocation = location;
        this.lastLocationAt = OffsetDateTime.now();
    }

    public void updateCategories(Set<MissionCategory> categories) {
        this.workerCategories.clear();
        if (categories != null) {
            this.workerCategories.addAll(categories);
        }
    }

    public boolean isPolyvalent() {
        return workerCategories.isEmpty();
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
