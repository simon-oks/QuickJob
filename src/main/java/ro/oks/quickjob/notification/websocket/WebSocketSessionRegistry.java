package ro.oks.quickjob.notification.websocket;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class WebSocketSessionRegistry {

    private final Map<UUID, String> connectedUsers = new ConcurrentHashMap<>();

    @EventListener
    public void onConnect(SessionConnectEvent event) {
        Principal principal = event.getUser();
        if (principal != null) {
            UUID userId = UUID.fromString(principal.getName());
            String sessionId = (String) event.getMessage().getHeaders().get("simpSessionId");
            connectedUsers.put(userId, sessionId);
        }
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        Principal principal = event.getUser();
        if (principal != null) {
            UUID userId = UUID.fromString(principal.getName());
            connectedUsers.remove(userId);
        }
    }

    public boolean isConnected(UUID userId) {
        return connectedUsers.containsKey(userId);
    }
}
