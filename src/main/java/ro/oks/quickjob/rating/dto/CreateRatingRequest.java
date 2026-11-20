package ro.oks.quickjob.rating.dto;

import java.util.UUID;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ro.oks.quickjob.rating.RatedRole;

public record CreateRatingRequest(
        @NotNull(message = "{validation.rating.role_required}")
        RatedRole roleRated,

        @NotNull(message = "{validation.rating.score_required}")
        @Min(value = 1, message = "{validation.rating.score_min}")
        @Max(value = 5, message = "{validation.rating.score_max}")
        Integer score,

        @Size(max = 500, message = "{validation.rating.comment_max_size}")
        String comment,

        UUID ratedUserId
) {
}
