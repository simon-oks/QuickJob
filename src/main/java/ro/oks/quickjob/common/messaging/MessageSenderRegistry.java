package ro.oks.quickjob.common.messaging;

import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class MessageSenderRegistry {

    private final List<MessageSender> senders;

    public MessageSenderRegistry(List<MessageSender> senders) {
        this.senders = senders;
    }

    public MessageSender getSender(String channel) {
        return senders.stream()
                .filter(s -> s.supports(channel))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No MessageSender for channel: " + channel));
    }
}
