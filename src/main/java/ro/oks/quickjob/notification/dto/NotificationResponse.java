package ro.oks.quickjob.notification.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import ro.oks.quickjob.notification.NotificationType;

public record NotificationResponse(
        UUID id,
        NotificationType type,
        Object payload,
        boolean read,
        OffsetDateTime createdAt
) {
}
