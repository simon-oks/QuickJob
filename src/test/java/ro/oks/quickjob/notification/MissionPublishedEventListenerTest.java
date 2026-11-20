package ro.oks.quickjob.notification;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Point;
import ro.oks.quickjob.common.geo.GeoUtils;
import ro.oks.quickjob.mission.Mission;
import ro.oks.quickjob.mission.MissionCategory;
import ro.oks.quickjob.mission.MissionProperties;
import ro.oks.quickjob.mission.MissionProperties.MissionConfig;
import ro.oks.quickjob.mission.MissionProperties.MoneyConfig;
import ro.oks.quickjob.mission.MissionPublishedEvent;
import ro.oks.quickjob.mission.MissionService;
import ro.oks.quickjob.user.User;
import ro.oks.quickjob.user.UserService;

class MissionPublishedEventListenerTest {

    private MissionService missionService;
    private UserService userService;
    private NotificationService notificationService;
    private MissionPublishedEventListener listener;

    private static final UUID MISSION_ID = UUID.randomUUID();
    private static final UUID CLIENT_ID = UUID.randomUUID();
    private static final UUID WORKER_1_ID = UUID.randomUUID();
    private static final UUID WORKER_2_ID = UUID.randomUUID();
    private static final UUID WORKER_3_ID = UUID.randomUUID();
    private static final Point LOME = GeoUtils.point(6.1319, 1.2228);

    @BeforeEach
    void setUp() {
        missionService = mock(MissionService.class);
        userService = mock(UserService.class);
        notificationService = mock(NotificationService.class);

        var missionConfig = new MissionConfig(60, 120, 48, 5000, 20000, 5);
        var moneyConfig = new MoneyConfig(null, 10);
        var properties = new MissionProperties(missionConfig, moneyConfig);

        listener = new MissionPublishedEventListener(missionService, userService, notificationService, properties);
    }

    @Test
    void notifies_all_nearby_workers_without_category_filter() {
        Mission mission = createMission(null);
        when(missionService.getById(MISSION_ID)).thenReturn(mission);
        when(missionService.distanceTo(eq(MISSION_ID), anyDouble(), anyDouble())).thenReturn(1500.0);
        when(userService.findNearbyWorkers(any(), eq(5000.0), eq(CLIENT_ID)))
                .thenReturn(List.of(createWorker(WORKER_1_ID, Set.of()), createWorker(WORKER_2_ID, Set.of())));

        listener.onMissionPublished(new MissionPublishedEvent(MISSION_ID, null));

        verify(notificationService, times(2)).createAndPush(any(), eq(NotificationType.NEW_MISSION), any());
        verify(notificationService).createAndPush(eq(WORKER_1_ID), eq(NotificationType.NEW_MISSION), any());
        verify(notificationService).createAndPush(eq(WORKER_2_ID), eq(NotificationType.NEW_MISSION), any());
    }

    @Test
    void filters_workers_by_category() {
        Mission mission = createMission(null);
        when(missionService.getById(MISSION_ID)).thenReturn(mission);
        when(missionService.distanceTo(eq(MISSION_ID), anyDouble(), anyDouble())).thenReturn(2000.0);

        User deliveryWorker = createWorker(WORKER_1_ID, Set.of(MissionCategory.DELIVERY));
        User cleaningWorker = createWorker(WORKER_2_ID, Set.of(MissionCategory.CLEANING));
        User polyvalentWorker = createWorker(WORKER_3_ID, Set.of());

        when(userService.findNearbyWorkers(any(), anyDouble(), eq(CLIENT_ID)))
                .thenReturn(List.of(deliveryWorker, cleaningWorker, polyvalentWorker));

        // On cible uniquement DELIVERY
        listener.onMissionPublished(new MissionPublishedEvent(MISSION_ID, Set.of(MissionCategory.DELIVERY)));

        // deliveryWorker et polyvalentWorker notifiés, cleaningWorker exclu
        verify(notificationService, times(2)).createAndPush(any(), eq(NotificationType.NEW_MISSION), any());
        verify(notificationService).createAndPush(eq(WORKER_1_ID), eq(NotificationType.NEW_MISSION), any());
        verify(notificationService).createAndPush(eq(WORKER_3_ID), eq(NotificationType.NEW_MISSION), any());
        verify(notificationService, never()).createAndPush(eq(WORKER_2_ID), any(), any());
    }

    @Test
    void uses_custom_notification_radius() {
        Mission mission = createMission(10000);
        when(missionService.getById(MISSION_ID)).thenReturn(mission);
        when(userService.findNearbyWorkers(any(), eq(10000.0), eq(CLIENT_ID)))
                .thenReturn(List.of());

        listener.onMissionPublished(new MissionPublishedEvent(MISSION_ID, null));

        verify(userService).findNearbyWorkers(any(), eq(10000.0), eq(CLIENT_ID));
    }

    @Test
    void caps_radius_at_max() {
        Mission mission = createMission(50000); // > max 20000
        when(missionService.getById(MISSION_ID)).thenReturn(mission);
        when(userService.findNearbyWorkers(any(), eq(20000.0), eq(CLIENT_ID)))
                .thenReturn(List.of());

        listener.onMissionPublished(new MissionPublishedEvent(MISSION_ID, null));

        verify(userService).findNearbyWorkers(any(), eq(20000.0), eq(CLIENT_ID));
    }

    @Test
    void uses_default_radius_when_null() {
        Mission mission = createMission(null);
        when(missionService.getById(MISSION_ID)).thenReturn(mission);
        when(userService.findNearbyWorkers(any(), eq(5000.0), eq(CLIENT_ID)))
                .thenReturn(List.of());

        listener.onMissionPublished(new MissionPublishedEvent(MISSION_ID, null));

        verify(userService).findNearbyWorkers(any(), eq(5000.0), eq(CLIENT_ID));
    }

    @Test
    void no_workers_means_no_notifications() {
        Mission mission = createMission(null);
        when(missionService.getById(MISSION_ID)).thenReturn(mission);
        when(userService.findNearbyWorkers(any(), anyDouble(), eq(CLIENT_ID)))
                .thenReturn(List.of());

        listener.onMissionPublished(new MissionPublishedEvent(MISSION_ID, null));

        verify(notificationService, never()).createAndPush(any(), any(), any());
    }

    @Test
    void polyvalent_worker_receives_notification_regardless_of_target_categories() {
        Mission mission = createMission(null);
        when(missionService.getById(MISSION_ID)).thenReturn(mission);
        when(missionService.distanceTo(eq(MISSION_ID), anyDouble(), anyDouble())).thenReturn(500.0);

        User polyvalent = createWorker(WORKER_1_ID, Set.of());
        when(userService.findNearbyWorkers(any(), anyDouble(), eq(CLIENT_ID)))
                .thenReturn(List.of(polyvalent));

        listener.onMissionPublished(new MissionPublishedEvent(MISSION_ID, Set.of(MissionCategory.GUARDING)));

        verify(notificationService).createAndPush(eq(WORKER_1_ID), eq(NotificationType.NEW_MISSION), any());
    }

    private Mission createMission(Integer notificationRadiusMeters) {
        Mission mission = new Mission(
                MISSION_ID, CLIENT_ID, MissionCategory.DELIVERY, "Livraison colis",
                "Description", LOME, "Grand Marché, Lomé",
                OffsetDateTime.now().plusHours(1), 60, 1500L, "XOF", 1);
        mission.setNotificationRadiusMeters(notificationRadiusMeters);
        return mission;
    }

    private User createWorker(UUID id, Set<MissionCategory> categories) {
        User user = new User(id, "+22890000001", "Worker");
        Point location = GeoUtils.point(6.1350, 1.2250);
        user.updateLocation(location);
        user.updateCategories(categories);
        return user;
    }
}
