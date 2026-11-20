package ro.oks.quickjob.application;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.oks.quickjob.application.dto.CreateApplicationRequest;
import ro.oks.quickjob.common.error.BusinessException;
import ro.oks.quickjob.common.error.ErrorCode;
import ro.oks.quickjob.mission.Mission;
import ro.oks.quickjob.mission.MissionService;
import ro.oks.quickjob.mission.MissionStatus;
import ro.oks.quickjob.notification.NotificationService;
import ro.oks.quickjob.notification.NotificationType;
import ro.oks.quickjob.notification.dto.ApplicationStatusPayload;

@Service
@Transactional(readOnly = true)
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final ApplicationMapper applicationMapper;
    private final MissionService missionService;
    private final NotificationService notificationService;

    public ApplicationService(ApplicationRepository applicationRepository,
                              ApplicationMapper applicationMapper,
                              MissionService missionService,
                              NotificationService notificationService) {
        this.applicationRepository = applicationRepository;
        this.applicationMapper = applicationMapper;
        this.missionService = missionService;
        this.notificationService = notificationService;
    }

    @Transactional
    public Application apply(CreateApplicationRequest request, UUID workerId) {
        Mission mission = missionService.getById(request.missionId());

        if (mission.getStatus() != MissionStatus.PUBLISHED) {
            throw new BusinessException(
                    ErrorCode.APPLICATION_MISSION_NOT_PUBLISHED,
                    "application.mission_not_published",
                    HttpStatus.CONFLICT);
        }

        if (mission.isFullyAssigned()) {
            throw new BusinessException(
                    ErrorCode.MISSION_FULLY_ASSIGNED,
                    "mission.fully_assigned",
                    HttpStatus.CONFLICT);
        }

        if (mission.getClientId().equals(workerId)) {
            throw new BusinessException(
                    ErrorCode.APPLICATION_OWN_MISSION,
                    "application.own_mission",
                    HttpStatus.BAD_REQUEST);
        }

        if (applicationRepository.existsByMissionIdAndWorkerId(request.missionId(), workerId)) {
            throw new BusinessException(
                    ErrorCode.APPLICATION_ALREADY_EXISTS,
                    "application.already_exists",
                    HttpStatus.CONFLICT);
        }

        Application application = applicationMapper.toEntity(request, workerId);
        return applicationRepository.save(application);
    }

    @Transactional
    public Application accept(UUID applicationId, UUID clientId) {
        Application application = getById(applicationId);
        Mission mission = missionService.getById(application.getMissionId());
        requireMissionOwner(mission, clientId);

        if (mission.isFullyAssigned()) {
            throw new BusinessException(
                    ErrorCode.MISSION_FULLY_ASSIGNED,
                    "mission.fully_assigned",
                    HttpStatus.CONFLICT);
        }

        application.accept();
        mission.assign(application.getWorkerId());

        notifyWorker(application, mission, NotificationType.APPLICATION_ACCEPTED);

        // Rejeter automatiquement les autres candidatures en attente seulement si la mission est complète
        if (mission.isFullyAssigned()) {
            List<Application> rejected = applicationRepository.findByMissionId(mission.getId()).stream()
                    .filter(a -> a.getStatus() == ApplicationStatus.PENDING && !a.getId().equals(applicationId))
                    .toList();
            for (Application a : rejected) {
                a.reject();
                notifyWorker(a, mission, NotificationType.APPLICATION_REJECTED);
            }
        }

        return application;
    }

    @Transactional
    public Application reject(UUID applicationId, UUID clientId) {
        Application application = getById(applicationId);
        Mission mission = missionService.getById(application.getMissionId());
        requireMissionOwner(mission, clientId);

        application.reject();
        notifyWorker(application, mission, NotificationType.APPLICATION_REJECTED);
        return application;
    }

    @Transactional
    public Application cancel(UUID applicationId, UUID workerId) {
        Application application = getById(applicationId);
        if (!application.getWorkerId().equals(workerId)) {
            throw new BusinessException(
                    ErrorCode.APPLICATION_ACCESS_DENIED,
                    "application.access_denied",
                    HttpStatus.FORBIDDEN);
        }
        application.cancel();
        return application;
    }

    public Application getById(UUID id) {
        return applicationRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.APPLICATION_NOT_FOUND,
                        "application.not_found",
                        HttpStatus.NOT_FOUND));
    }

    public Page<Application> getByMissionId(UUID missionId, UUID clientId, Pageable pageable) {
        Mission mission = missionService.getById(missionId);
        requireMissionOwner(mission, clientId);
        return applicationRepository.findByMissionId(missionId, pageable);
    }

    public Page<Application> getByWorkerId(UUID workerId, Pageable pageable) {
        return applicationRepository.findByWorkerId(workerId, pageable);
    }

    private void requireMissionOwner(Mission mission, UUID clientId) {
        if (!mission.getClientId().equals(clientId)) {
            throw new BusinessException(
                    ErrorCode.APPLICATION_ACCESS_DENIED,
                    "application.access_denied.owner",
                    HttpStatus.FORBIDDEN);
        }
    }

    private void notifyWorker(Application application, Mission mission, NotificationType type) {
        var payload = new ApplicationStatusPayload(
                application.getId(),
                mission.getId(),
                mission.getTitle(),
                application.getStatus()
        );
        notificationService.createAndPush(application.getWorkerId(), type, payload);
    }
}
