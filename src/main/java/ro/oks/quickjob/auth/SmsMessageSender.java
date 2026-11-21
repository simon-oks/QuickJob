package ro.oks.quickjob.auth;

import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ro.oks.quickjob.common.messaging.MessageSender;

/**
 * Implémentation factice pour le développement et les tests.
 * Logue le message au lieu de l'envoyer réellement par SMS.
 */
@Component
public class SmsMessageSender implements MessageSender {

    private static final Logger log = LoggerFactory.getLogger(SmsMessageSender.class);

    private final SmsSender smsSender;

    public SmsMessageSender(SmsSender smsSender) {
        this.smsSender = smsSender;
    }

    @Override
    public boolean supports(String channel) {
        return "sms".equals(channel);
    }

    @Override
    public void sendOtp(String recipient, String code, Locale locale) {
        String message = "fr".equals(locale.getLanguage())
                ? "Votre code QuickJob : " + code
                : "Your QuickJob code: " + code;
        smsSender.send(recipient, message);
    }

    @Override
    public void sendWelcome(String recipient, String displayName, Locale locale) {
        String message = "fr".equals(locale.getLanguage())
                ? "Bienvenue sur QuickJob, " + displayName + " !"
                : "Welcome to QuickJob, " + displayName + "!";
        smsSender.send(recipient, message);
    }
}
