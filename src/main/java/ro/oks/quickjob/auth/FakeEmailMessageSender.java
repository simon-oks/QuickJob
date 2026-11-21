package ro.oks.quickjob.auth;

import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import ro.oks.quickjob.common.messaging.MessageSender;

/**
 * Implémentation factice pour le développement et les tests.
 * Logue au lieu d'envoyer un email.
 */
@Component
@ConditionalOnProperty(name = "quickjob.mail.enabled", havingValue = "false", matchIfMissing = true)
public class FakeEmailMessageSender implements MessageSender {

    private static final Logger log = LoggerFactory.getLogger(FakeEmailMessageSender.class);

    @Override
    public boolean supports(String channel) {
        return "email".equals(channel);
    }

    @Override
    public void sendOtp(String recipient, String code, Locale locale) {
        log.info("[FAKE EMAIL] OTP à {} : {}", recipient, code);
    }

    @Override
    public void sendWelcome(String recipient, String displayName, Locale locale) {
        log.info("[FAKE EMAIL] Bienvenue à {} ({})", recipient, displayName);
    }
}
