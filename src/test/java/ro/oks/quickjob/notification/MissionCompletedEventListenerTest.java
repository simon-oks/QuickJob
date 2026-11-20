package ro.oks.quickjob.notification;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ro.oks.quickjob.common.i18n.MessageService;
import ro.oks.quickjob.mission.MissionCompletedEvent;
import ro.oks.quickjob.user.User;
import ro.oks.quickjob.user.UserService;

class MissionCompletedEventListenerTest {

    private NotificationService notificationService;
    private UserService userService;
    private MessageService messageService;
    private MissionCompletedEventListener listener;

    private static final UUID MISSION_ID = UUID.randomUUID();
    private static final UUID CLIENT_ID = UUID.randomUUID();
    private static final UUID WORKER_1_ID = UUID.randomUUID();
    private static final UUID WORKER_2_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        notificationService = mock(NotificationService.class);
        userService = mock(UserService.class);
        messageService = mock(MessageService.class);
        listener = new MissionCompletedEventListener(notificationService, userService, messageService);

        when(messageService.get(eq("notification.mission_completed"), any(Locale.class)))
                .thenReturn("Notez votre experience");

        User clientUser = mock(User.class);
        when(clientUser.getPreferredLanguage()).thenReturn("fr");
        when(userService.getById(CLIENT_ID)).thenReturn(clientUser);

        User worker1User = mock(User.class);
        when(worker1User.getPreferredLanguage()).thenReturn("fr");
        when(userService.getById(WORKER_1_ID)).thenReturn(worker1User);

        User worker2User = mock(User.class);
        when(worker2User.getPreferredLanguage()).thenReturn("fr");
        when(userService.getById(WORKER_2_ID)).thenReturn(worker2User);
    }

    @Test
    void notifies_client_and_all_workers_on_completion() {
        var event = new MissionCompletedEvent(MISSION_ID, CLIENT_ID, Set.of(WORKER_1_ID, WORKER_2_ID));

        listener.onMissionCompleted(event);

        // 1 client + 2 workers = 3 notifications
        verify(notificationService, times(3))
                .createAndPush(any(), eq(NotificationType.MISSION_COMPLETED), any());
        verify(notificationService).createAndPush(eq(CLIENT_ID), eq(NotificationType.MISSION_COMPLETED), any());
        verify(notificationService).createAndPush(eq(WORKER_1_ID), eq(NotificationType.MISSION_COMPLETED), any());
        verify(notificationService).createAndPush(eq(WORKER_2_ID), eq(NotificationType.MISSION_COMPLETED), any());
    }

    @Test
    void notifies_client_and_single_worker() {
        var event = new MissionCompletedEvent(MISSION_ID, CLIENT_ID, Set.of(WORKER_1_ID));

        listener.onMissionCompleted(event);

        verify(notificationService, times(2))
                .createAndPush(any(), eq(NotificationType.MISSION_COMPLETED), any());
        verify(notificationService).createAndPush(eq(CLIENT_ID), eq(NotificationType.MISSION_COMPLETED), any());
        verify(notificationService).createAndPush(eq(WORKER_1_ID), eq(NotificationType.MISSION_COMPLETED), any());
    }
}
