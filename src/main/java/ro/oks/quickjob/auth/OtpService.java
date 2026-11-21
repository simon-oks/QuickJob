package ro.oks.quickjob.auth;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ro.oks.quickjob.common.messaging.MessageSenderRegistry;

/**
 * Gestion des codes OTP en mémoire.
 * En production, les OTP seront stockés dans Redis ou en base avec TTL.
 */
@Service
public class OtpService {

    private static final Logger log = LoggerFactory.getLogger(OtpService.class);
    private static final int OTP_LENGTH = 6;
    private static final long OTP_VALIDITY_SECONDS = 300; // 5 minutes
    private static final int MAX_ATTEMPTS = 5;

    private final SecureRandom random = new SecureRandom();
    private final Map<String, OtpEntry> otpStore = new ConcurrentHashMap<>();
    private final MessageSenderRegistry messageSenderRegistry;

    public OtpService(MessageSenderRegistry messageSenderRegistry) {
        this.messageSenderRegistry = messageSenderRegistry;
    }

    public void generateAndSend(String identifier, String channel, Locale locale) {
        String code = generateCode();
        otpStore.put(identifier, new OtpEntry(code, Instant.now(), 0));
        messageSenderRegistry.getSender(channel).sendOtp(identifier, code, locale);
        log.info("OTP envoyé via {} à {}", channel, maskIdentifier(identifier, channel));
    }

    public boolean verify(String identifier, String code) {
        OtpEntry entry = otpStore.get(identifier);
        if (entry == null) {
            return false;
        }
        if (entry.attempts >= MAX_ATTEMPTS) {
            otpStore.remove(identifier);
            return false;
        }
        if (Instant.now().isAfter(entry.createdAt.plusSeconds(OTP_VALIDITY_SECONDS))) {
            otpStore.remove(identifier);
            return false;
        }
        if (entry.code.equals(code)) {
            otpStore.remove(identifier);
            return true;
        }
        otpStore.put(identifier, new OtpEntry(entry.code, entry.createdAt, entry.attempts + 1));
        return false;
    }

    private String generateCode() {
        int bound = (int) Math.pow(10, OTP_LENGTH);
        return String.format("%0" + OTP_LENGTH + "d", random.nextInt(bound));
    }

    private String maskIdentifier(String identifier, String channel) {
        if ("email".equals(channel)) {
            int atIndex = identifier.indexOf('@');
            if (atIndex <= 2) return "***" + identifier.substring(atIndex);
            return identifier.substring(0, 2) + "***" + identifier.substring(atIndex);
        }
        if (identifier.length() <= 6) return "****";
        return identifier.substring(0, 4) + "****" + identifier.substring(identifier.length() - 4);
    }

    private record OtpEntry(String code, Instant createdAt, int attempts) {}
}
