package ro.oks.quickjob.common.messaging;

import java.util.Locale;

public interface MessageSender {

    boolean supports(String channel);

    void sendOtp(String recipient, String code, Locale locale);

    void sendWelcome(String recipient, String displayName, Locale locale);
}
