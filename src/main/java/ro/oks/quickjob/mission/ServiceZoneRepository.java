package ro.oks.quickjob.mission;

import java.util.UUID;

import org.locationtech.jts.geom.Point;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ServiceZoneRepository extends JpaRepository<ServiceZone, UUID> {

    @Query(value = """
            SELECT EXISTS(
                SELECT 1 FROM service_zones sz
                WHERE sz.active = true
                  AND ST_Covers(sz.boundary, :point)
            )
            """, nativeQuery = true)
    boolean existsActiveZoneContaining(@Param("point") Point point);
}
