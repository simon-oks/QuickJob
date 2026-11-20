package ro.oks.quickjob.notification;

import java.util.Locale;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import ro.oks.quickjob.common.i18n.MessageService;
import ro.oks.quickjob.mission.MissionCompletedEvent;
import ro.oks.quickjob.notification.dto.MissionCompletedPayload;
import ro.oks.quickjob.user.User;
import ro.oks.quickjob.user.UserService;

@Component
public class MissionCompletedEventListener {

    private static final Logger log = LoggerFactory.getLogger(MissionCompletedEventListener.class);

    private final NotificationService notificationService;
    private final UserService userService;
    private final MessageService messageService;

    public MissionCompletedEventListener(NotificationService notificationService,
                                          UserService userService,
                                          MessageService messageService) {
        this.notificationService = notificationService;
        this.userService = userService;
        this.messageService = messageService;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMissionCompleted(MissionCompletedEvent event) {
        try {
            sendNotification(event.clientId(), event.missionId());

            for (UUID workerId : event.workerIds()) {
                sendNotification(workerId, event.missionId());
            }

            log.info("Mission {} completee — notifications envoyees a {} utilisateurs",
                    event.missionId(), 1 + event.workerIds().size());
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi des notifications de completion pour la mission {}",
                    event.missionId(), e);
        }
    }

    private void sendNotification(UUID userId, UUID missionId) {
        User user = userService.getById(userId);
        Locale locale = Locale.forLanguageTag(user.getPreferredLanguage());
        String message = messageService.get("notification.mission_completed", locale);

        MissionCompletedPayload payload = new MissionCompletedPayload(missionId, message);
        notificationService.createAndPush(userId, NotificationType.MISSION_COMPLETED, payload);
    }
}
