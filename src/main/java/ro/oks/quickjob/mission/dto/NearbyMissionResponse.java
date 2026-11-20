package ro.oks.quickjob.mission.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import ro.oks.quickjob.mission.MissionCategory;

/**
 * Reponse pour la recherche de proximite.
 * Contient des coordonnees approximatives (bruitees +-200m) pour l'affichage sur carte.
 * Distance arrondie a 100m pour preserver la vie privee.
 */
public record NearbyMissionResponse(
        UUID id,
        MissionCategory category,
        String title,
        String description,
        String addressLabel,
        long distanceMeters,
        Double approximateLatitude,
        Double approximateLongitude,
        OffsetDateTime startAt,
        int durationMinutes,
        long amountMinor,
        String currency,
        int requiredWorkers,
        int assignedWorkersCount,
        OffsetDateTime createdAt,
        String deliveryAddressLabel,
        String additionalInfo
) {
}
