package ro.oks.quickjob.mission;

import java.util.Set;
import java.util.UUID;

public record MissionPublishedEvent(UUID missionId, Set<MissionCategory> targetWorkerCategories) {
}
