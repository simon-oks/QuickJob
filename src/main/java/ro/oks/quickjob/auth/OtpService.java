package ro.oks.quickjob.auth;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

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
    private final SmsSender smsSender;

    public OtpService(SmsSender smsSender) {
        this.smsSender = smsSender;
    }

    public void generateAndSend(String phoneNumber) {
        String code = generateCode();
        otpStore.put(phoneNumber, new OtpEntry(code, Instant.now(), 0));
        smsSender.send(phoneNumber, "Votre code QuickJob : " + code);
        log.info("OTP envoyé à {}", maskPhone(phoneNumber));
    }

    public boolean verify(String phoneNumber, String code) {
        OtpEntry entry = otpStore.get(phoneNumber);
        if (entry == null) {
            return false;
        }
        if (entry.attempts >= MAX_ATTEMPTS) {
            otpStore.remove(phoneNumber);
            return false;
        }
        if (Instant.now().isAfter(entry.createdAt.plusSeconds(OTP_VALIDITY_SECONDS))) {
            otpStore.remove(phoneNumber);
            return false;
        }
        if (entry.code.equals(code)) {
            otpStore.remove(phoneNumber);
            return true;
        }
        otpStore.put(phoneNumber, new OtpEntry(entry.code, entry.createdAt, entry.attempts + 1));
        return false;
    }

    private String generateCode() {
        int bound = (int) Math.pow(10, OTP_LENGTH);
        return String.format("%0" + OTP_LENGTH + "d", random.nextInt(bound));
    }

    private String maskPhone(String phone) {
        if (phone.length() <= 6) return "****";
        return phone.substring(0, 4) + "****" + phone.substring(phone.length() - 4);
    }

    private record OtpEntry(String code, Instant createdAt, int attempts) {}
}
