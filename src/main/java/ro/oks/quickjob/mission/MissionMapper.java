package ro.oks.quickjob.mission;

import java.util.UUID;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ro.oks.quickjob.common.geo.GeoUtils;
import ro.oks.quickjob.mission.dto.CreateMissionRequest;
import ro.oks.quickjob.mission.dto.MissionResponse;
import ro.oks.quickjob.mission.dto.NearbyMissionResponse;
import ro.oks.quickjob.rating.RatingService;

@Component
public class MissionMapper {

    private final RatingService ratingService;

    public MissionMapper(@Lazy RatingService ratingService) {
        this.ratingService = ratingService;
    }

    public Mission toEntity(CreateMissionRequest request, UUID clientId) {
        int requiredWorkers = request.requiredWorkers() != null
                ? request.requiredWorkers()
                : 1;

        Mission mission = new Mission(
                UUID.randomUUID(),
                clientId,
                request.category(),
                request.title(),
                request.description(),
                GeoUtils.point(request.latitude(), request.longitude()),
                request.addressLabel(),
                request.startAt(),
                request.durationMinutes(),
                request.amountMinor(),
                request.currency(),
                requiredWorkers
        );
        mission.setNotificationRadiusMeters(request.notificationRadiusMeters());
        if (request.deliveryLatitude() != null && request.deliveryLongitude() != null) {
            mission.setDeliveryLocation(GeoUtils.point(request.deliveryLatitude(), request.deliveryLongitude()));
        }
        mission.setDeliveryAddressLabel(request.deliveryAddressLabel());
        mission.setAdditionalInfo(request.additionalInfo());
        return mission;
    }

    public MissionResponse toResponse(Mission mission) {
        return toResponse(mission, true, null);
    }

    public MissionResponse toResponse(Mission mission, UUID requestingUserId) {
        return toResponse(mission, true, requestingUserId);
    }

    public MissionResponse toResponse(Mission mission, boolean includeLocation) {
        return toResponse(mission, includeLocation, null);
    }

    public MissionResponse toResponse(Mission mission, boolean includeLocation, UUID requestingUserId) {
        Boolean myRatingSubmitted = null;
        if (mission.getStatus() == MissionStatus.COMPLETED && requestingUserId != null) {
            myRatingSubmitted = ratingService.hasUserRatedMission(mission.getId(), requestingUserId);
        }

        Double deliveryLat = null;
        Double deliveryLon = null;
        if (includeLocation && mission.getDeliveryLocation() != null) {
            deliveryLat = GeoUtils.latitude(mission.getDeliveryLocation());
            deliveryLon = GeoUtils.longitude(mission.getDeliveryLocation());
        }

        return new MissionResponse(
                mission.getId(),
                mission.getClientId(),
                mission.getCategory(),
                mission.getTitle(),
                mission.getDescription(),
                includeLocation ? GeoUtils.latitude(mission.getLocation()) : null,
                includeLocation ? GeoUtils.longitude(mission.getLocation()) : null,
                mission.getAddressLabel(),
                mission.getStartAt(),
                mission.getDurationMinutes(),
                mission.getAmountMinor(),
                mission.getCurrency(),
                mission.getStatus(),
                mission.getRequiredWorkers(),
                mission.getAssignedWorkersCount(),
                mission.getCreatedAt(),
                myRatingSubmitted,
                deliveryLat,
                deliveryLon,
                mission.getDeliveryAddressLabel(),
                mission.getAdditionalInfo()
        );
    }

    public NearbyMissionResponse toNearbyResponse(Mission mission, double distanceMeters) {
        double[] approxCoords = GeoUtils.approximateCoordinates(
                GeoUtils.latitude(mission.getLocation()),
                GeoUtils.longitude(mission.getLocation()),
                mission.getId());

        return new NearbyMissionResponse(
                mission.getId(),
                mission.getCategory(),
                mission.getTitle(),
                mission.getDescription(),
                mission.getAddressLabel(),
                Math.round(distanceMeters / 100.0) * 100,
                approxCoords[0],
                approxCoords[1],
                mission.getStartAt(),
                mission.getDurationMinutes(),
                mission.getAmountMinor(),
                mission.getCurrency(),
                mission.getRequiredWorkers(),
                mission.getAssignedWorkersCount(),
                mission.getCreatedAt(),
                mission.getDeliveryAddressLabel(),
                mission.getAdditionalInfo()
        );
    }
}
