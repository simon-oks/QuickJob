package ro.oks.quickjob.notification.websocket;

import java.security.Principal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.jsonwebtoken.Claims;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import ro.oks.quickjob.auth.JwtService;

@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;

    public WebSocketAuthInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = extractToken(accessor);
            if (token != null) {
                Claims claims = jwtService.parseToken(token);
                if (claims != null) {
                    UUID userId = UUID.fromString(claims.getSubject());
                    String roles = claims.get("roles", String.class);
                    List<SimpleGrantedAuthority> authorities = Arrays.stream(roles.split(","))
                            .map(r -> new SimpleGrantedAuthority("ROLE_" + r.trim()))
                            .toList();

                    Principal principal = new StompPrincipal(userId);
                    var auth = new UsernamePasswordAuthenticationToken(principal, null, authorities);
                    accessor.setUser(auth);
                }
            }
        }
        return message;
    }

    private String extractToken(StompHeaderAccessor accessor) {
        // Essayer d'abord le header STOMP Authorization
        List<String> authHeaders = accessor.getNativeHeader("Authorization");
        if (authHeaders != null && !authHeaders.isEmpty()) {
            String header = authHeaders.getFirst();
            if (header.startsWith("Bearer ")) {
                return header.substring(7);
            }
        }
        // Sinon, le token est passé dans les attributs de session (via query param)
        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        if (sessionAttributes != null) {
            Object token = sessionAttributes.get("token");
            if (token instanceof String s) {
                return s;
            }
        }
        return null;
    }
}
