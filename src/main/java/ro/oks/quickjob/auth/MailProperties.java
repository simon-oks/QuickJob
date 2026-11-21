package ro.oks.quickjob.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "quickjob.mail")
public record MailProperties(
        String from,
        boolean enabled
) {}
