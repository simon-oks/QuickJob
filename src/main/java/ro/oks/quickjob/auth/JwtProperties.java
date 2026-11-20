package ro.oks.quickjob.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "quickjob.jwt")
public record JwtProperties(
        String secret,
        long accessTokenExpirationMinutes,
        long refreshTokenExpirationDays
) {
    public JwtProperties {
        if (accessTokenExpirationMinutes <= 0) accessTokenExpirationMinutes = 15;
        if (refreshTokenExpirationDays <= 0) refreshTokenExpirationDays = 30;
    }
}
