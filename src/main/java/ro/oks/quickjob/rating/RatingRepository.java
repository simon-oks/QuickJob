package ro.oks.quickjob.rating;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RatingRepository extends JpaRepository<Rating, UUID> {

    boolean existsByMissionIdAndRaterId(UUID missionId, UUID raterId);

    @Query("""
            SELECT COALESCE(AVG(CAST(r.score AS double)), 0.0)
            FROM Rating r
            WHERE r.ratedId = :userId AND r.roleRated = :role
            """)
    double calculateAverageRating(@Param("userId") UUID userId, @Param("role") RatedRole role);

    @Query("""
            SELECT COUNT(r)
            FROM Rating r
            WHERE r.ratedId = :userId AND r.roleRated = :role
            """)
    long countRatings(@Param("userId") UUID userId, @Param("role") RatedRole role);
}
