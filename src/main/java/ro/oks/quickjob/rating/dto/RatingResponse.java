package ro.oks.quickjob.rating.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import ro.oks.quickjob.rating.RatedRole;

public record RatingResponse(
        UUID id,
        UUID missionId,
        UUID raterId,
        UUID ratedId,
        RatedRole roleRated,
        int score,
        String comment,
        OffsetDateTime createdAt
) {
}
