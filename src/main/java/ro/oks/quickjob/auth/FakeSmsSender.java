package ro.oks.quickjob.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Implémentation factice pour le développement et les tests.
 * Logue le message au lieu de l'envoyer réellement.
 */
@Component
public class FakeSmsSender implements SmsSender {

    private static final Logger log = LoggerFactory.getLogger(FakeSmsSender.class);

    @Override
    public void send(String phoneNumber, String message) {
        log.info("[FAKE SMS] À {} : {}", phoneNumber, message);
    }
}
