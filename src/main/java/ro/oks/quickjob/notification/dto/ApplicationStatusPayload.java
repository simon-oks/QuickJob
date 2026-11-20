package ro.oks.quickjob.notification.dto;

import java.util.UUID;

import ro.oks.quickjob.application.ApplicationStatus;

public record ApplicationStatusPayload(
        UUID applicationId,
        UUID missionId,
        String missionTitle,
        ApplicationStatus status
) {
}
