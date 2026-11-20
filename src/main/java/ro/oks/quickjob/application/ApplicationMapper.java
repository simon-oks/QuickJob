package ro.oks.quickjob.application;

import java.util.UUID;

import org.springframework.stereotype.Component;
import ro.oks.quickjob.application.dto.ApplicationResponse;
import ro.oks.quickjob.application.dto.CreateApplicationRequest;

@Component
public class ApplicationMapper {

    public Application toEntity(CreateApplicationRequest request, UUID workerId) {
        return new Application(
                UUID.randomUUID(),
                request.missionId(),
                workerId,
                request.message()
        );
    }

    public ApplicationResponse toResponse(Application application) {
        return new ApplicationResponse(
                application.getId(),
                application.getMissionId(),
                application.getWorkerId(),
                application.getStatus(),
                application.getMessage(),
                application.getCreatedAt()
        );
    }
}
