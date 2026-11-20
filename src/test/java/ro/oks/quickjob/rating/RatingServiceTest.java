package ro.oks.quickjob.rating;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ro.oks.quickjob.common.error.BusinessException;
import ro.oks.quickjob.common.geo.GeoUtils;
import ro.oks.quickjob.mission.Mission;
import ro.oks.quickjob.mission.MissionCategory;
import ro.oks.quickjob.mission.MissionService;
import ro.oks.quickjob.rating.dto.CreateRatingRequest;

class RatingServiceTest {

    private RatingRepository ratingRepository;
    private MissionService missionService;
    private RatingService ratingService;

    private static final UUID MISSION_ID = UUID.randomUUID();
    private static final UUID CLIENT_ID = UUID.randomUUID();
    private static final UUID WORKER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        ratingRepository = mock(RatingRepository.class);
        missionService = mock(MissionService.class);
        RatingMapper ratingMapper = new RatingMapper();
        ratingService = new RatingService(ratingRepository, missionService, ratingMapper);
    }

    @Test
    void worker_rates_client_successfully() {
        Mission mission = createCompletedMission();
        when(missionService.getById(MISSION_ID)).thenReturn(mission);
        when(ratingRepository.existsByMissionIdAndRaterId(MISSION_ID, WORKER_ID)).thenReturn(false);
        when(ratingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = new CreateRatingRequest(RatedRole.CLIENT, 4, "Bon client", null);
        Rating rating = ratingService.createRating(MISSION_ID, request, WORKER_ID);

        assertThat(rating.getRaterId()).isEqualTo(WORKER_ID);
        assertThat(rating.getRatedId()).isEqualTo(CLIENT_ID);
        assertThat(rating.getRoleRated()).isEqualTo(RatedRole.CLIENT);
        assertThat(rating.getScore()).isEqualTo(4);
    }

    @Test
    void client_rates_worker_successfully() {
        Mission mission = createCompletedMission();
        when(missionService.getById(MISSION_ID)).thenReturn(mission);
        when(ratingRepository.existsByMissionIdAndRaterId(MISSION_ID, CLIENT_ID)).thenReturn(false);
        when(ratingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = new CreateRatingRequest(RatedRole.WORKER, 5, "Excellent travail", null);
        Rating rating = ratingService.createRating(MISSION_ID, request, CLIENT_ID);

        assertThat(rating.getRaterId()).isEqualTo(CLIENT_ID);
        assertThat(rating.getRatedId()).isEqualTo(WORKER_ID);
        assertThat(rating.getRoleRated()).isEqualTo(RatedRole.WORKER);
        assertThat(rating.getScore()).isEqualTo(5);
    }

    @Test
    void rejects_duplicate_rating() {
        Mission mission = createCompletedMission();
        when(missionService.getById(MISSION_ID)).thenReturn(mission);
        when(ratingRepository.existsByMissionIdAndRaterId(MISSION_ID, WORKER_ID)).thenReturn(true);

        var request = new CreateRatingRequest(RatedRole.CLIENT, 3, null, null);

        assertThatThrownBy(() -> ratingService.createRating(MISSION_ID, request, WORKER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("rating.already_exists");
    }

    @Test
    void rejects_rating_when_mission_not_completed() {
        Mission mission = createPublishedMission();
        when(missionService.getById(MISSION_ID)).thenReturn(mission);

        var request = new CreateRatingRequest(RatedRole.CLIENT, 4, null, null);

        assertThatThrownBy(() -> ratingService.createRating(MISSION_ID, request, WORKER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("rating.mission_not_completed");
    }

    @Test
    void rejects_rating_when_window_expired() {
        Mission mission = createCompletedMissionWithOldCompletion();
        when(missionService.getById(MISSION_ID)).thenReturn(mission);

        var request = new CreateRatingRequest(RatedRole.CLIENT, 4, null, null);

        assertThatThrownBy(() -> ratingService.createRating(MISSION_ID, request, WORKER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("rating.window_expired");
    }

    @Test
    void rejects_unauthorized_rater() {
        Mission mission = createCompletedMission();
        when(missionService.getById(MISSION_ID)).thenReturn(mission);

        UUID strangerId = UUID.randomUUID();
        when(ratingRepository.existsByMissionIdAndRaterId(MISSION_ID, strangerId)).thenReturn(false);

        var request = new CreateRatingRequest(RatedRole.CLIENT, 3, null, null);

        assertThatThrownBy(() -> ratingService.createRating(MISSION_ID, request, strangerId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("rating.unauthorized.not_participant");
    }

    @Test
    void rejects_worker_trying_to_rate_another_worker() {
        Mission mission = createCompletedMission();
        when(missionService.getById(MISSION_ID)).thenReturn(mission);
        when(ratingRepository.existsByMissionIdAndRaterId(MISSION_ID, WORKER_ID)).thenReturn(false);

        var request = new CreateRatingRequest(RatedRole.WORKER, 4, null, null);

        assertThatThrownBy(() -> ratingService.createRating(MISSION_ID, request, WORKER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("rating.unauthorized.client_only");
    }

    @Test
    void stats_return_empty_when_no_ratings() {
        when(ratingRepository.calculateAverageRating(CLIENT_ID, RatedRole.CLIENT)).thenReturn(0.0);
        when(ratingRepository.countRatings(CLIENT_ID, RatedRole.CLIENT)).thenReturn(0L);

        var stats = ratingService.getClientRatingStats(CLIENT_ID);

        assertThat(stats.averageScore()).isEqualTo(0.0);
        assertThat(stats.totalRatings()).isEqualTo(0);
    }

    @Test
    void stats_return_correct_values() {
        when(ratingRepository.calculateAverageRating(WORKER_ID, RatedRole.WORKER)).thenReturn(4.333);
        when(ratingRepository.countRatings(WORKER_ID, RatedRole.WORKER)).thenReturn(3L);

        var stats = ratingService.getWorkerRatingStats(WORKER_ID);

        assertThat(stats.averageScore()).isEqualTo(4.3);
        assertThat(stats.totalRatings()).isEqualTo(3);
    }

    private Mission createCompletedMission() {
        Mission mission = new Mission(
                MISSION_ID, CLIENT_ID, MissionCategory.DELIVERY, "Livraison",
                "Description", GeoUtils.point(6.1319, 1.2228), "Grand Marche, Lome",
                OffsetDateTime.now().minusHours(2), 60, 1500L, "XOF", 1);
        mission.assign(WORKER_ID);
        mission.start();
        mission.complete();
        return mission;
    }

    private Mission createCompletedMissionWithOldCompletion() {
        Mission mission = new Mission(
                MISSION_ID, CLIENT_ID, MissionCategory.DELIVERY, "Livraison",
                "Description", GeoUtils.point(6.1319, 1.2228), "Grand Marche, Lome",
                OffsetDateTime.now().minusDays(10), 60, 1500L, "XOF", 1);
        mission.assign(WORKER_ID);
        mission.start();
        mission.complete();
        // completedAt is set by complete() to now(), but we need it to be old
        // Use reflection to set completedAt to 8 days ago
        try {
            var field = Mission.class.getDeclaredField("completedAt");
            field.setAccessible(true);
            field.set(mission, OffsetDateTime.now().minusDays(8));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return mission;
    }

    private Mission createPublishedMission() {
        return new Mission(
                MISSION_ID, CLIENT_ID, MissionCategory.DELIVERY, "Livraison",
                "Description", GeoUtils.point(6.1319, 1.2228), "Grand Marche, Lome",
                OffsetDateTime.now().plusHours(1), 60, 1500L, "XOF", 1);
    }
}
