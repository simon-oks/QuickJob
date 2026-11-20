package ro.oks.quickjob.mission;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "quickjob")
public record MissionProperties(
        MissionConfig mission,
        MoneyConfig money
) {
    public record MissionConfig(
            int minDuration,
            int maxDuration,
            int maxAdvanceHours,
            int defaultSearchRadiusMeters,
            int maxSearchRadiusMeters,
            int maxRequiredWorkers
    ) {
    }

    public record MoneyConfig(
            Map<String, Long> minRemuneration,
            int platformCommissionPercent
    ) {
    }
}
