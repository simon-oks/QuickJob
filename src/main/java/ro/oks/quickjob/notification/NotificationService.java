package ro.oks.quickjob.notification;

import java.util.UUID;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.oks.quickjob.common.error.BusinessException;
import ro.oks.quickjob.common.error.ErrorCode;
import ro.oks.quickjob.notification.dto.NotificationResponse;
import ro.oks.quickjob.notification.websocket.WebSocketSessionRegistry;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final WebSocketSessionRegistry sessionRegistry;
    private final ObjectMapper objectMapper;

    public NotificationService(NotificationRepository notificationRepository,
                               NotificationMapper notificationMapper,
                               SimpMessagingTemplate messagingTemplate,
                               WebSocketSessionRegistry sessionRegistry,
                               ObjectMapper objectMapper) {
        this.notificationRepository = notificationRepository;
        this.notificationMapper = notificationMapper;
        this.messagingTemplate = messagingTemplate;
        this.sessionRegistry = sessionRegistry;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void createAndPush(UUID userId, NotificationType type, Object payload) {
        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (JacksonException e) {
            throw new RuntimeException("notification.serialization_error", e);
        }

        Notification notification = new Notification(UUID.randomUUID(), userId, type, json);
        notificationRepository.save(notification);

        NotificationResponse response = notificationMapper.toResponse(notification);
        if (sessionRegistry.isConnected(userId)) {
            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    "/queue/notifications",
                    response);
        }
    }

    public Page<NotificationResponse> getByUserId(UUID userId, boolean unreadOnly, Pageable pageable) {
        Page<Notification> page;
        if (unreadOnly) {
            page = notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId, pageable);
        } else {
            page = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        }
        return page.map(notificationMapper::toResponse);
    }

    @Transactional
    public void markAsRead(UUID notificationId, UUID userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.NOTIFICATION_NOT_FOUND,
                        "notification.not_found",
                        HttpStatus.NOT_FOUND));

        if (!notification.getUserId().equals(userId)) {
            throw new BusinessException(
                    ErrorCode.AUTH_ACCESS_DENIED,
                    "notification.access_denied",
                    HttpStatus.FORBIDDEN);
        }

        notification.markAsRead();
    }

    @Transactional
    public void markAllAsRead(UUID userId) {
        notificationRepository.markAllAsRead(userId);
    }

    public long countUnread(UUID userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }
}
