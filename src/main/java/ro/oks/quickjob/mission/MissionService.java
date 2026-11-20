package ro.oks.quickjob.mission;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.locationtech.jts.geom.Point;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.oks.quickjob.common.error.BusinessException;
import ro.oks.quickjob.common.error.ErrorCode;
import ro.oks.quickjob.common.geo.GeoUtils;
import ro.oks.quickjob.mission.dto.CreateMissionRequest;
import ro.oks.quickjob.user.User;
import ro.oks.quickjob.user.UserService;

@Service
@Transactional(readOnly = true)
public class MissionService {

    private final MissionRepository missionRepository;
    private final ServiceZoneRepository serviceZoneRepository;
    private final UserService userService;
    private final MissionMapper missionMapper;
    private final MissionProperties properties;
    private final ApplicationEventPublisher eventPublisher;

    public MissionService(MissionRepository missionRepository,
                          ServiceZoneRepository serviceZoneRepository,
                          UserService userService,
                          MissionMapper missionMapper,
                          MissionProperties properties,
                          ApplicationEventPublisher eventPublisher) {
        this.missionRepository = missionRepository;
        this.serviceZoneRepository = serviceZoneRepository;
        this.userService = userService;
        this.missionMapper = missionMapper;
        this.properties = properties;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Mission create(CreateMissionRequest request, UUID clientId) {
        User client = userService.getById(clientId);
        if (!client.isPhoneVerified()) {
            throw new BusinessException(
                    ErrorCode.USER_PHONE_NOT_VERIFIED,
                    "user.phone_not_verified",
                    HttpStatus.FORBIDDEN);
        }

        validateDuration(request.durationMinutes());
        validateStartAt(request.startAt());
        validateLocation(request.latitude(), request.longitude());
        validateRemuneration(request.amountMinor(), request.currency());
        validateRequiredWorkers(request.requiredWorkers());
        validateNotificationRadius(request.notificationRadiusMeters());
        validateDeliveryFields(request);

        Mission mission = missionMapper.toEntity(request, clientId);
        mission = missionRepository.save(mission);

        eventPublisher.publishEvent(new MissionPublishedEvent(mission.getId(), request.targetWorkerCategories()));

        return mission;
    }

    public Mission getById(UUID id) {
        return missionRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.MISSION_NOT_FOUND,
                        "mission.not_found",
                        HttpStatus.NOT_FOUND));
    }

    public Page<Mission> findNearby(double latitude, double longitude,
                                     Double radiusMeters, UUID workerId,
                                     Pageable pageable) {
        double radius = radiusMeters != null ? radiusMeters
                : properties.mission().defaultSearchRadiusMeters();
        if (radius > properties.mission().maxSearchRadiusMeters()) {
            radius = properties.mission().maxSearchRadiusMeters();
        }
        Point point = GeoUtils.point(latitude, longitude);
        return missionRepository.findNearby(point, radius, workerId, pageable);
    }

    @Transactional
    public Mission start(UUID missionId, UUID userId) {
        Mission mission = getById(missionId);
        requireClientOrAssignedWorker(mission, userId);
        mission.start();
        return mission;
    }

    @Transactional
    public Mission complete(UUID missionId, UUID userId) {
        Mission mission = getById(missionId);
        requireClientOrAssignedWorker(mission, userId);
        mission.complete();

        eventPublisher.publishEvent(new MissionCompletedEvent(
                mission.getId(),
                mission.getClientId(),
                mission.getWorkerIds()));

        return mission;
    }

    @Transactional
    public Mission cancel(UUID missionId, UUID clientId) {
        Mission mission = getById(missionId);
        requireClientOwner(mission, clientId);
        mission.cancel();
        return mission;
    }

    public Page<Mission> findByClientId(UUID clientId, Pageable pageable) {
        return missionRepository.findByClientId(clientId, pageable);
    }

    public Page<Mission> findByWorkerId(UUID workerId, Pageable pageable) {
        return missionRepository.findByWorkerId(workerId, pageable);
    }

    public Page<Mission> findFeed(UUID workerId, Pageable pageable) {
        User worker = userService.getById(workerId);
        boolean allCategories = worker.isPolyvalent();
        Set<String> categories;
        if (allCategories) {
            categories = Set.of();
        } else {
            categories = worker.getWorkerCategories().stream()
                    .map(Enum::name)
                    .collect(Collectors.toSet());
        }
        return missionRepository.findFeed(workerId, allCategories, categories, pageable);
    }

    public double distanceTo(UUID missionId, double latitude, double longitude) {
        Point point = GeoUtils.point(latitude, longitude);
        Double distance = missionRepository.distanceTo(missionId, point);
        return distance != null ? distance : -1;
    }

    private void validateDuration(int durationMinutes) {
        int min = properties.mission().minDuration();
        int max = properties.mission().maxDuration();
        if (durationMinutes < min || durationMinutes > max) {
            throw new BusinessException(
                    ErrorCode.MISSION_INVALID_DURATION,
                    "mission.invalid_duration",
                    HttpStatus.BAD_REQUEST, min, max);
        }
    }

    private void validateStartAt(OffsetDateTime startAt) {
        OffsetDateTime now = OffsetDateTime.now();
        if (!startAt.isAfter(now)) {
            throw new BusinessException(
                    ErrorCode.MISSION_INVALID_START_TIME,
                    "mission.invalid_start_time.past",
                    HttpStatus.BAD_REQUEST);
        }
        int maxAdvanceHours = properties.mission().maxAdvanceHours();
        if (startAt.isAfter(now.plusHours(maxAdvanceHours))) {
            throw new BusinessException(
                    ErrorCode.MISSION_INVALID_START_TIME,
                    "mission.invalid_start_time.too_far",
                    HttpStatus.BAD_REQUEST, maxAdvanceHours);
        }
    }

    private void validateLocation(double latitude, double longitude) {
        Point point = GeoUtils.point(latitude, longitude);
        if (!serviceZoneRepository.existsActiveZoneContaining(point)) {
            throw new BusinessException(
                    ErrorCode.MISSION_OUTSIDE_SERVICE_ZONE,
                    "mission.outside_service_zone",
                    HttpStatus.BAD_REQUEST);
        }
    }

    private void validateRequiredWorkers(Integer requiredWorkers) {
        if (requiredWorkers == null) {
            return;
        }
        int max = properties.mission().maxRequiredWorkers();
        if (requiredWorkers > max) {
            throw new BusinessException(
                    ErrorCode.MISSION_INVALID_REQUIRED_WORKERS,
                    "mission.invalid_required_workers",
                    HttpStatus.BAD_REQUEST, max);
        }
    }

    private void validateNotificationRadius(Integer notificationRadiusMeters) {
        if (notificationRadiusMeters == null) {
            return;
        }
        int max = properties.mission().maxSearchRadiusMeters();
        if (notificationRadiusMeters > max) {
            throw new BusinessException(
                    ErrorCode.MISSION_INVALID_NOTIFICATION_RADIUS,
                    "mission.invalid_notification_radius",
                    HttpStatus.BAD_REQUEST, max);
        }
    }

    private void validateDeliveryFields(CreateMissionRequest request) {
        boolean hasLat = request.deliveryLatitude() != null;
        boolean hasLon = request.deliveryLongitude() != null;
        if (hasLat != hasLon) {
            throw new BusinessException(
                    ErrorCode.MISSION_INCOMPLETE_DELIVERY_COORDS,
                    "mission.delivery_coords_incomplete",
                    HttpStatus.BAD_REQUEST);
        }
        if (hasLat && hasLon) {
            validateLocation(request.deliveryLatitude(), request.deliveryLongitude());
        }
    }

    private void requireClientOwner(Mission mission, UUID userId) {
        if (!mission.getClientId().equals(userId)) {
            throw new BusinessException(
                    ErrorCode.AUTH_ACCESS_DENIED,
                    "auth.access_denied.mission_owner",
                    HttpStatus.FORBIDDEN);
        }
    }

    private void requireClientOrAssignedWorker(Mission mission, UUID userId) {
        if (!mission.getClientId().equals(userId) && !mission.hasWorker(userId)) {
            throw new BusinessException(
                    ErrorCode.AUTH_ACCESS_DENIED,
                    "auth.access_denied.mission_participant",
                    HttpStatus.FORBIDDEN);
        }
    }

    private void validateRemuneration(long amountMinor, String currencyCode) {
        var minMap = properties.money().minRemuneration();
        if (minMap != null && minMap.containsKey(currencyCode)) {
            long min = minMap.get(currencyCode);
            if (amountMinor < min) {
                throw new BusinessException(
                        ErrorCode.MISSION_INVALID_REMUNERATION,
                        "mission.invalid_remuneration",
                        HttpStatus.BAD_REQUEST, currencyCode, min);
            }
        }
    }
}
