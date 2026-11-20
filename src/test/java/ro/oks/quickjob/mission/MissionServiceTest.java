package ro.oks.quickjob.mission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import ro.oks.quickjob.common.error.BusinessException;
import ro.oks.quickjob.common.error.ErrorCode;

class MissionServiceTest {

    private static final UUID WORKER_ID = UUID.randomUUID();

    // --- Transitions de statut (mission à 1 worker) ---

    @Test
    void mission_transitions_published_to_assigned() {
        Mission mission = createPublishedMission(1);
        mission.assign(WORKER_ID);
        assertThat(mission.getStatus()).isEqualTo(MissionStatus.ASSIGNED);
        assertThat(mission.hasWorker(WORKER_ID)).isTrue();
        assertThat(mission.getAssignedWorkersCount()).isEqualTo(1);
    }

    @Test
    void mission_transitions_assigned_to_in_progress() {
        Mission mission = createPublishedMission(1);
        mission.assign(WORKER_ID);
        mission.start();
        assertThat(mission.getStatus()).isEqualTo(MissionStatus.IN_PROGRESS);
    }

    @Test
    void mission_transitions_in_progress_to_completed() {
        Mission mission = createPublishedMission(1);
        mission.assign(WORKER_ID);
        mission.start();
        mission.complete();
        assertThat(mission.getStatus()).isEqualTo(MissionStatus.COMPLETED);
    }

    @Test
    void mission_transitions_published_to_cancelled() {
        Mission mission = createPublishedMission(1);
        mission.cancel();
        assertThat(mission.getStatus()).isEqualTo(MissionStatus.CANCELLED);
    }

    @Test
    void mission_transitions_published_to_expired() {
        Mission mission = createPublishedMission(1);
        mission.expire();
        assertThat(mission.getStatus()).isEqualTo(MissionStatus.EXPIRED);
    }

    @Test
    void cannot_assign_non_published_mission() {
        Mission mission = createPublishedMission(1);
        mission.assign(WORKER_ID);
        assertThatThrownBy(() -> mission.assign(UUID.randomUUID()))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                });
    }

    @Test
    void cannot_cancel_completed_mission() {
        Mission mission = createPublishedMission(1);
        mission.assign(WORKER_ID);
        mission.start();
        mission.complete();
        assertThatThrownBy(mission::cancel)
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void cannot_expire_non_published_mission() {
        Mission mission = createPublishedMission(1);
        mission.assign(WORKER_ID);
        assertThatThrownBy(mission::expire)
                .isInstanceOf(BusinessException.class);
    }

    // --- Multi-workers ---

    @Test
    void multi_worker_mission_stays_published_until_fully_assigned() {
        UUID worker1 = UUID.randomUUID();
        UUID worker2 = UUID.randomUUID();
        UUID worker3 = UUID.randomUUID();

        Mission mission = createPublishedMission(3);

        mission.assign(worker1);
        assertThat(mission.getStatus()).isEqualTo(MissionStatus.PUBLISHED);
        assertThat(mission.getAssignedWorkersCount()).isEqualTo(1);
        assertThat(mission.isFullyAssigned()).isFalse();

        mission.assign(worker2);
        assertThat(mission.getStatus()).isEqualTo(MissionStatus.PUBLISHED);
        assertThat(mission.getAssignedWorkersCount()).isEqualTo(2);
        assertThat(mission.isFullyAssigned()).isFalse();

        mission.assign(worker3);
        assertThat(mission.getStatus()).isEqualTo(MissionStatus.ASSIGNED);
        assertThat(mission.getAssignedWorkersCount()).isEqualTo(3);
        assertThat(mission.isFullyAssigned()).isTrue();
    }

    @Test
    void cannot_assign_same_worker_twice() {
        Mission mission = createPublishedMission(2);
        mission.assign(WORKER_ID);

        assertThatThrownBy(() -> mission.assign(WORKER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getCode()).isEqualTo(ErrorCode.MISSION_WORKER_ALREADY_ASSIGNED);
                });
    }

    @Test
    void cannot_assign_more_workers_than_required() {
        UUID worker1 = UUID.randomUUID();
        UUID worker2 = UUID.randomUUID();
        UUID extraWorker = UUID.randomUUID();

        Mission mission = createPublishedMission(2);
        mission.assign(worker1);
        mission.assign(worker2);

        // Mission is now ASSIGNED, so assign() should fail with INVALID_TRANSITION
        assertThatThrownBy(() -> mission.assign(extraWorker))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void has_worker_returns_false_for_non_assigned_worker() {
        Mission mission = createPublishedMission(2);
        mission.assign(WORKER_ID);

        assertThat(mission.hasWorker(WORKER_ID)).isTrue();
        assertThat(mission.hasWorker(UUID.randomUUID())).isFalse();
    }

    // --- Helpers ---

    private Mission createPublishedMission(int requiredWorkers) {
        return new Mission(
                UUID.randomUUID(),
                UUID.randomUUID(),
                MissionCategory.DELIVERY,
                "Livraison test",
                "Description test",
                ro.oks.quickjob.common.geo.GeoUtils.point(6.1319, 1.2228),
                "Grand Marché, Lomé",
                java.time.OffsetDateTime.now().plusHours(1),
                60,
                1500,
                "XOF",
                requiredWorkers
        );
    }
}
