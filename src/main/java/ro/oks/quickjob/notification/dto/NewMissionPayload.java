package ro.oks.quickjob.notification.dto;

import java.util.UUID;

import ro.oks.quickjob.mission.MissionCategory;

public record NewMissionPayload(
        UUID missionId,
        String title,
        MissionCategory category,
        String addressLabel,
        long amountMinor,
        String currency,
        long distanceMeters,
        int requiredWorkers
) {
}
