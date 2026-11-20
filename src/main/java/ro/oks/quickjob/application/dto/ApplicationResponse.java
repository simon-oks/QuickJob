package ro.oks.quickjob.application.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import ro.oks.quickjob.application.ApplicationStatus;

public record ApplicationResponse(
        UUID id,
        UUID missionId,
        UUID workerId,
        ApplicationStatus status,
        String message,
        OffsetDateTime createdAt
) {
}
