package ro.oks.quickjob.mission;

import java.time.OffsetDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tâche planifiée qui expire les missions PUBLISHED dont la date de début est dépassée.
 */
@Component
public class MissionExpirationScheduler {

    private static final Logger log = LoggerFactory.getLogger(MissionExpirationScheduler.class);

    private final MissionRepository missionRepository;

    public MissionExpirationScheduler(MissionRepository missionRepository) {
        this.missionRepository = missionRepository;
    }

    @Scheduled(fixedDelayString = "${quickjob.mission.expiration-check-interval-ms:60000}")
    @Transactional
    public void expireOverdueMissions() {
        List<Mission> overdue = missionRepository.findExpiredPublished(OffsetDateTime.now());
        if (overdue.isEmpty()) {
            return;
        }
        for (Mission mission : overdue) {
            mission.expire();
        }
        log.info("{} mission(s) expirée(s)", overdue.size());
    }
}
