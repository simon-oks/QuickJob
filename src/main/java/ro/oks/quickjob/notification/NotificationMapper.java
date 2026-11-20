package ro.oks.quickjob.notification;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import ro.oks.quickjob.notification.dto.NotificationResponse;

@Component
public class NotificationMapper {

    private final ObjectMapper objectMapper;

    public NotificationMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public NotificationResponse toResponse(Notification notification) {
        Object payload;
        try {
            payload = objectMapper.readValue(notification.getPayload(), Object.class);
        } catch (JacksonException e) {
            payload = notification.getPayload();
        }
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                payload,
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}
