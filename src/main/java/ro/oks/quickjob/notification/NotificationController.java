package ro.oks.quickjob.notification;

import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ro.oks.quickjob.notification.dto.NotificationResponse;

@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notifications", description = "Gestion des notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @Operation(summary = "Mes notifications",
               description = "Liste paginée des notifications de l'utilisateur connecté.")
    public Page<NotificationResponse> getMyNotifications(
            @AuthenticationPrincipal UUID userId,
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            Pageable pageable) {
        return notificationService.getByUserId(userId, unreadOnly, pageable);
    }

    @PatchMapping("/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Marquer une notification comme lue")
    public void markAsRead(@PathVariable UUID id,
                           @AuthenticationPrincipal UUID userId) {
        notificationService.markAsRead(id, userId);
    }

    @PostMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Marquer toutes les notifications comme lues")
    public void markAllAsRead(@AuthenticationPrincipal UUID userId) {
        notificationService.markAllAsRead(userId);
    }
}
