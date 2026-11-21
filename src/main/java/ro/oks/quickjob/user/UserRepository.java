package ro.oks.quickjob.user;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.locationtech.jts.geom.Point;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByPhoneNumber(String phoneNumber);

    Optional<User> findByEmail(String email);

    @Query(value = """
            SELECT u.*
            FROM users u
            WHERE u.roles LIKE '%WORKER%'
              AND u.last_known_location IS NOT NULL
              AND ST_DWithin(u.last_known_location, :location, :radiusMeters)
              AND u.id != :excludeUserId
            """, nativeQuery = true)
    List<User> findNearbyWorkers(@Param("location") Point location,
                                 @Param("radiusMeters") double radiusMeters,
                                 @Param("excludeUserId") UUID excludeUserId);
}
