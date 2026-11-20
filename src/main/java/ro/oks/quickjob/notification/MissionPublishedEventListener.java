package ro.oks.quickjob.notification;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import ro.oks.quickjob.common.geo.GeoUtils;
import ro.oks.quickjob.mission.Mission;
import ro.oks.quickjob.mission.MissionCategory;
import ro.oks.quickjob.mission.MissionProperties;
import ro.oks.quickjob.mission.MissionService;
import ro.oks.quickjob.notification.dto.NewMissionPayload;
import ro.oks.quickjob.user.User;
import ro.oks.quickjob.user.UserService;

@Component
public class MissionPublishedEventListener {

    private static final Logger log = LoggerFactory.getLogger(MissionPublishedEventListener.class);

    private final MissionService missionService;
    private final UserService userService;
    private final NotificationService notificationService;
    private final MissionProperties properties;

    public MissionPublishedEventListener(MissionService missionService,
                                          UserService userService,
                                          NotificationService notificationService,
                                          MissionProperties properties) {
        this.missionService = missionService;
        this.userService = userService;
        this.notificationService = notificationService;
        this.properties = properties;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMissionPublished(ro.oks.quickjob.mission.MissionPublishedEvent event) {
        try {
            Mission mission = missionService.getById(event.missionId());

            double radiusMeters = resolveRadius(mission);

            List<User> nearbyWorkers = userService.findNearbyWorkers(
                    mission.getLocation(),
                    radiusMeters,
                    mission.getClientId());

            Set<MissionCategory> targetCategories = event.targetWorkerCategories();
            boolean filterByCategory = targetCategories != null && !targetCategories.isEmpty();

            int notified = 0;
            for (User worker : nearbyWorkers) {
                if (filterByCategory && !worker.isPolyvalent()
                        && Collections.disjoint(worker.getWorkerCategories(), targetCategories)) {
                    continue;
                }

                double distance = computeDistance(mission, worker);

                NewMissionPayload payload = new NewMissionPayload(
                        mission.getId(),
                        mission.getTitle(),
                        mission.getCategory(),
                        mission.getAddressLabel(),
                        mission.getAmountMinor(),
                        mission.getCurrency(),
                        Math.round(distance / 100.0) * 100,
                        mission.getRequiredWorkers()
                );

                notificationService.createAndPush(worker.getId(), NotificationType.NEW_MISSION, payload);
                notified++;
            }

            log.info("Mission {} publiée — {} workers notifiés dans un rayon de {} m",
                    mission.getId(), notified, (int) radiusMeters);
        } catch (Exception e) {
            log.error("Erreur lors de la notification pour la mission {}", event.missionId(), e);
        }
    }

    private double resolveRadius(Mission mission) {
        double radius;
        if (mission.getNotificationRadiusMeters() != null) {
            radius = mission.getNotificationRadiusMeters();
        } else {
            radius = properties.mission().defaultSearchRadiusMeters();
        }
        double max = properties.mission().maxSearchRadiusMeters();
        return Math.min(radius, max);
    }

    private double computeDistance(Mission mission, User worker) {
        if (worker.getLastKnownLocation() == null) {
            return 0;
        }
        double workerLat = GeoUtils.latitude(worker.getLastKnownLocation());
        double workerLon = GeoUtils.longitude(worker.getLastKnownLocation());
        Double distance = missionService.distanceTo(mission.getId(), workerLat, workerLon);
        return distance != null ? distance : 0;
    }
}
