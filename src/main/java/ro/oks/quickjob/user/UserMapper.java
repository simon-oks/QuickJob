package ro.oks.quickjob.user;

import org.springframework.stereotype.Component;
import ro.oks.quickjob.rating.RatingService;
import ro.oks.quickjob.user.dto.UserResponse;

@Component
public class UserMapper {

    private final RatingService ratingService;

    public UserMapper(RatingService ratingService) {
        this.ratingService = ratingService;
    }

    public UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getPhoneNumber(),
                user.getDisplayName(),
                user.getRoleSet(),
                user.getWorkerCategories(),
                user.isPhoneVerified(),
                user.getPreferredLanguage(),
                ratingService.getClientRatingStats(user.getId()),
                ratingService.getWorkerRatingStats(user.getId()),
                user.getCreatedAt()
        );
    }
}
