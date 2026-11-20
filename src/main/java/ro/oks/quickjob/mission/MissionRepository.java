package ro.oks.quickjob.mission;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.locationtech.jts.geom.Point;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MissionRepository extends JpaRepository<Mission, UUID> {

    @Query(value = """
            SELECT m.*, ST_Distance(m.location, :point) AS distance_m
            FROM missions m
            WHERE m.status = 'PUBLISHED'
              AND ST_DWithin(m.location, :point, :radiusMeters)
              AND NOT EXISTS (
                  SELECT 1 FROM applications a
                  WHERE a.mission_id = m.id AND a.worker_id = :workerId
              )
            ORDER BY distance_m
            """,
            countQuery = """
            SELECT count(*)
            FROM missions m
            WHERE m.status = 'PUBLISHED'
              AND ST_DWithin(m.location, :point, :radiusMeters)
              AND NOT EXISTS (
                  SELECT 1 FROM applications a
                  WHERE a.mission_id = m.id AND a.worker_id = :workerId
              )
            """,
            nativeQuery = true)
    Page<Mission> findNearby(@Param("point") Point point,
                             @Param("radiusMeters") double radiusMeters,
                             @Param("workerId") UUID workerId,
                             Pageable pageable);

    @Query(value = """
            SELECT ST_Distance(m.location, :point)
            FROM missions m
            WHERE m.id = :missionId
            """, nativeQuery = true)
    Double distanceTo(@Param("missionId") UUID missionId, @Param("point") Point point);

    Page<Mission> findByClientId(UUID clientId, Pageable pageable);

    @Query("SELECT m FROM Mission m JOIN m.workerIds w WHERE w = :workerId")
    Page<Mission> findByWorkerId(@Param("workerId") UUID workerId, Pageable pageable);

    @Query("SELECT m FROM Mission m WHERE m.status = ro.oks.quickjob.mission.MissionStatus.PUBLISHED AND m.startAt < :cutoff")
    List<Mission> findExpiredPublished(@Param("cutoff") OffsetDateTime cutoff);

    @Query(value = """
            SELECT m.* FROM missions m
            WHERE m.status = 'PUBLISHED'
              AND (:allCategories = true OR m.category IN (:categories))
              AND NOT EXISTS (
                  SELECT 1 FROM applications a
                  WHERE a.mission_id = m.id AND a.worker_id = :workerId
              )
            ORDER BY m.created_at DESC
            """,
            countQuery = """
            SELECT count(*) FROM missions m
            WHERE m.status = 'PUBLISHED'
              AND (:allCategories = true OR m.category IN (:categories))
              AND NOT EXISTS (
                  SELECT 1 FROM applications a
                  WHERE a.mission_id = m.id AND a.worker_id = :workerId
              )
            """,
            nativeQuery = true)
    Page<Mission> findFeed(@Param("workerId") UUID workerId,
                           @Param("allCategories") boolean allCategories,
                           @Param("categories") Set<String> categories,
                           Pageable pageable);
}
