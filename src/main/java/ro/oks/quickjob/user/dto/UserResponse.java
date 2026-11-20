package ro.oks.quickjob.user.dto;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

import ro.oks.quickjob.mission.MissionCategory;
import ro.oks.quickjob.rating.dto.RatingStats;
import ro.oks.quickjob.user.Role;

public record UserResponse(
        UUID id,
        String phoneNumber,
        String displayName,
        Set<Role> roles,
        Set<MissionCategory> workerCategories,
        boolean phoneVerified,
        String preferredLanguage,
        RatingStats clientRating,
        RatingStats workerRating,
        OffsetDateTime createdAt
) {
}
