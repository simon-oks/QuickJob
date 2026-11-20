package ro.oks.quickjob.notification.dto;

import java.util.UUID;

public record MissionCompletedPayload(
        UUID missionId,
        String message
) {
}
