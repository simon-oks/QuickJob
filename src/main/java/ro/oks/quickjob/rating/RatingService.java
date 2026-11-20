package ro.oks.quickjob.rating;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.oks.quickjob.common.error.BusinessException;
import ro.oks.quickjob.common.error.ErrorCode;
import ro.oks.quickjob.mission.Mission;
import ro.oks.quickjob.mission.MissionService;
import ro.oks.quickjob.mission.MissionStatus;
import ro.oks.quickjob.rating.dto.CreateRatingRequest;
import ro.oks.quickjob.rating.dto.RatingStats;

@Service
@Transactional(readOnly = true)
public class RatingService {

    private static final int RATING_WINDOW_DAYS = 7;

    private final RatingRepository ratingRepository;
    private final MissionService missionService;
    private final RatingMapper ratingMapper;

    public RatingService(RatingRepository ratingRepository,
                         MissionService missionService,
                         RatingMapper ratingMapper) {
        this.ratingRepository = ratingRepository;
        this.missionService = missionService;
        this.ratingMapper = ratingMapper;
    }

    @Transactional
    public Rating createRating(UUID missionId, CreateRatingRequest request, UUID raterId) {
        Mission mission = missionService.getById(missionId);

        validateMissionCompleted(mission);
        validateRatingWindow(mission);

        if (ratingRepository.existsByMissionIdAndRaterId(missionId, raterId)) {
            throw new BusinessException(
                    ErrorCode.RATING_ALREADY_EXISTS,
                    "rating.already_exists",
                    HttpStatus.CONFLICT);
        }

        validateRaterAuthorization(mission, raterId);

        UUID ratedId = determineRatedUser(mission, raterId, request);

        if (raterId.equals(ratedId)) {
            throw new BusinessException(
                    ErrorCode.RATING_SELF_RATING,
                    "rating.self_rating",
                    HttpStatus.BAD_REQUEST);
        }

        Rating rating = ratingMapper.toEntity(request, missionId, raterId, ratedId);
        return ratingRepository.save(rating);
    }

    public RatingStats getClientRatingStats(UUID userId) {
        double avg = ratingRepository.calculateAverageRating(userId, RatedRole.CLIENT);
        long count = ratingRepository.countRatings(userId, RatedRole.CLIENT);
        return ratingMapper.toStats(avg, count);
    }

    public RatingStats getWorkerRatingStats(UUID userId) {
        double avg = ratingRepository.calculateAverageRating(userId, RatedRole.WORKER);
        long count = ratingRepository.countRatings(userId, RatedRole.WORKER);
        return ratingMapper.toStats(avg, count);
    }

    public boolean hasUserRatedMission(UUID missionId, UUID userId) {
        return ratingRepository.existsByMissionIdAndRaterId(missionId, userId);
    }

    private void validateMissionCompleted(Mission mission) {
        if (mission.getStatus() != MissionStatus.COMPLETED) {
            throw new BusinessException(
                    ErrorCode.RATING_MISSION_NOT_COMPLETED,
                    "rating.mission_not_completed",
                    HttpStatus.BAD_REQUEST);
        }
    }

    private void validateRatingWindow(Mission mission) {
        if (mission.getCompletedAt() == null) {
            throw new BusinessException(
                    ErrorCode.RATING_MISSION_NOT_COMPLETED,
                    "rating.no_completion_date",
                    HttpStatus.BAD_REQUEST);
        }
        OffsetDateTime deadline = mission.getCompletedAt().plusDays(RATING_WINDOW_DAYS);
        if (OffsetDateTime.now().isAfter(deadline)) {
            throw new BusinessException(
                    ErrorCode.RATING_WINDOW_EXPIRED,
                    "rating.window_expired",
                    HttpStatus.BAD_REQUEST, RATING_WINDOW_DAYS);
        }
    }

    private void validateRaterAuthorization(Mission mission, UUID raterId) {
        boolean isClient = mission.getClientId().equals(raterId);
        boolean isWorker = mission.hasWorker(raterId);
        if (!isClient && !isWorker) {
            throw new BusinessException(
                    ErrorCode.RATING_UNAUTHORIZED,
                    "rating.unauthorized.not_participant",
                    HttpStatus.FORBIDDEN);
        }
    }

    private UUID determineRatedUser(Mission mission, UUID raterId, CreateRatingRequest request) {
        if (request.roleRated() == RatedRole.CLIENT) {
            if (!mission.hasWorker(raterId)) {
                throw new BusinessException(
                        ErrorCode.RATING_UNAUTHORIZED,
                        "rating.unauthorized.worker_only",
                        HttpStatus.FORBIDDEN);
            }
            return mission.getClientId();
        } else {
            if (!mission.getClientId().equals(raterId)) {
                throw new BusinessException(
                        ErrorCode.RATING_UNAUTHORIZED,
                        "rating.unauthorized.client_only",
                        HttpStatus.FORBIDDEN);
            }
            if (request.ratedUserId() != null) {
                if (!mission.hasWorker(request.ratedUserId())) {
                    throw new BusinessException(
                            ErrorCode.RATING_UNAUTHORIZED,
                            "rating.unauthorized.worker_not_assigned",
                            HttpStatus.BAD_REQUEST);
                }
                return request.ratedUserId();
            }
            if (mission.getWorkerIds().size() == 1) {
                return mission.getWorkerIds().iterator().next();
            }
            throw new BusinessException(
                    ErrorCode.RATING_UNAUTHORIZED,
                    "rating.unauthorized.multi_worker",
                    HttpStatus.BAD_REQUEST);
        }
    }
}
