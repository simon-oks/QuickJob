package ro.oks.quickjob.mission;

import java.util.Set;
import java.util.UUID;

public record MissionCompletedEvent(
        UUID missionId,
        UUID clientId,
        Set<UUID> workerIds
) {
}
