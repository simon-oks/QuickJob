package ro.oks.quickjob.rating;

import java.util.UUID;

import org.springframework.stereotype.Component;
import ro.oks.quickjob.rating.dto.CreateRatingRequest;
import ro.oks.quickjob.rating.dto.RatingResponse;
import ro.oks.quickjob.rating.dto.RatingStats;

@Component
public class RatingMapper {

    public Rating toEntity(CreateRatingRequest request, UUID missionId,
                           UUID raterId, UUID ratedId) {
        return new Rating(
                UUID.randomUUID(),
                missionId,
                raterId,
                ratedId,
                request.roleRated(),
                request.score(),
                request.comment()
        );
    }

    public RatingResponse toResponse(Rating rating) {
        return new RatingResponse(
                rating.getId(),
                rating.getMissionId(),
                rating.getRaterId(),
                rating.getRatedId(),
                rating.getRoleRated(),
                rating.getScore(),
                rating.getComment(),
                rating.getCreatedAt()
        );
    }

    public RatingStats toStats(double average, long count) {
        if (count == 0) {
            return RatingStats.empty();
        }
        return new RatingStats(
                Math.round(average * 10.0) / 10.0,
                count
        );
    }
}
