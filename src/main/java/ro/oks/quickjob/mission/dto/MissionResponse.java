package ro.oks.quickjob.mission.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import ro.oks.quickjob.mission.MissionCategory;
import ro.oks.quickjob.mission.MissionStatus;

public record MissionResponse(
        UUID id,
        UUID clientId,
        MissionCategory category,
        String title,
        String description,
        Double latitude,
        Double longitude,
        String addressLabel,
        OffsetDateTime startAt,
        int durationMinutes,
        long amountMinor,
        String currency,
        MissionStatus status,
        int requiredWorkers,
        int assignedWorkersCount,
        OffsetDateTime createdAt,
        Boolean myRatingSubmitted,
        Double deliveryLatitude,
        Double deliveryLongitude,
        String deliveryAddressLabel,
        String additionalInfo
) {
}
