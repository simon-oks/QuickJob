package ro.oks.quickjob.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationRepository extends JpaRepository<Application, UUID> {

    List<Application> findByMissionId(UUID missionId);

    Page<Application> findByMissionId(UUID missionId, Pageable pageable);

    List<Application> findByWorkerId(UUID workerId);

    Page<Application> findByWorkerId(UUID workerId, Pageable pageable);

    Optional<Application> findByMissionIdAndWorkerId(UUID missionId, UUID workerId);

    boolean existsByMissionIdAndWorkerId(UUID missionId, UUID workerId);

    long countByMissionIdAndStatus(UUID missionId, ApplicationStatus status);
}
