package ro.oks.quickjob.auth;

import java.util.Locale;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import ro.oks.quickjob.common.messaging.MessageSender;

@Component
@ConditionalOnProperty(name = "quickjob.mail.enabled", havingValue = "true")
public class EmailMessageSender implements MessageSender {

    private static final Logger log = LoggerFactory.getLogger(EmailMessageSender.class);
    private static final String APP_NAME = "QuickJob";

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final MailProperties mailProperties;

    public EmailMessageSender(JavaMailSender mailSender, TemplateEngine templateEngine,
                              MailProperties mailProperties) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.mailProperties = mailProperties;
    }

    @Override
    public boolean supports(String channel) {
        return "email".equals(channel);
    }

    @Override
    public void sendOtp(String recipient, String code, Locale locale) {
        String lang = "fr".equals(locale.getLanguage()) ? "fr" : "en";
        String templateName = "otp-email-" + lang;

        Context context = new Context(locale);
        context.setVariable("code", code);
        context.setVariable("appName", APP_NAME);

        String subject = "fr".equals(lang)
                ? "Votre code de vérification QuickJob"
                : "Your QuickJob verification code";

        String html = templateEngine.process(templateName, context);
        sendHtmlEmail(recipient, subject, html);
        log.info("OTP email envoyé à {}", maskEmail(recipient));
    }

    @Override
    public void sendWelcome(String recipient, String displayName, Locale locale) {
        String lang = "fr".equals(locale.getLanguage()) ? "fr" : "en";
        String templateName = "welcome-email-" + lang;

        Context context = new Context(locale);
        context.setVariable("displayName", displayName);
        context.setVariable("appName", APP_NAME);

        String subject = "fr".equals(lang)
                ? "Bienvenue sur QuickJob !"
                : "Welcome to QuickJob!";

        String html = templateEngine.process(templateName, context);
        sendHtmlEmail(recipient, subject, html);
        log.info("Welcome email envoyé à {}", maskEmail(recipient));
    }

    private void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(mailProperties.from());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            log.error("Erreur envoi email à {}: {}", maskEmail(to), e.getMessage());
            throw new RuntimeException("Erreur d'envoi de l'email", e);
        }
    }

    private String maskEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex <= 2) return "***" + email.substring(atIndex);
        return email.substring(0, 2) + "***" + email.substring(atIndex);
    }
}
