package ro.oks.quickjob.rating;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ratings")
@Getter
@NoArgsConstructor
public class Rating {

    @Id
    private UUID id;

    @Column(name = "mission_id", nullable = false)
    private UUID missionId;

    @Column(name = "rater_id", nullable = false)
    private UUID raterId;

    @Column(name = "rated_id", nullable = false)
    private UUID ratedId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_rated", nullable = false, length = 10)
    private RatedRole roleRated;

    @Column(name = "score", nullable = false)
    private int score;

    @Column(name = "comment", length = 500)
    private String comment;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public Rating(UUID id, UUID missionId, UUID raterId, UUID ratedId,
                  RatedRole roleRated, int score, String comment) {
        this.id = id;
        this.missionId = missionId;
        this.raterId = raterId;
        this.ratedId = ratedId;
        this.roleRated = roleRated;
        this.score = score;
        this.comment = comment;
    }

    @PrePersist
    void prePersist() {
        this.createdAt = OffsetDateTime.now();
    }
}
