package ro.oks.quickjob.common.ratelimit;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Limiteur de débit en mémoire par clé (IP, numéro de téléphone, etc.).
 * Fenêtre glissante simplifiée : compteur réinitialisé à chaque période.
 */
public class RateLimiter {

    private final int maxRequests;
    private final long windowSeconds;
    private final Map<String, WindowCounter> counters = new ConcurrentHashMap<>();

    public RateLimiter(int maxRequests, long windowSeconds) {
        this.maxRequests = maxRequests;
        this.windowSeconds = windowSeconds;
    }

    public boolean tryAcquire(String key) {
        long now = Instant.now().getEpochSecond();
        WindowCounter counter = counters.compute(key, (k, existing) -> {
            if (existing == null || existing.isExpired(now, windowSeconds)) {
                return new WindowCounter(now);
            }
            return existing;
        });
        return counter.incrementAndCheck(maxRequests);
    }

    private static class WindowCounter {
        private final long windowStart;
        private final AtomicInteger count = new AtomicInteger(0);

        WindowCounter(long windowStart) {
            this.windowStart = windowStart;
        }

        boolean isExpired(long now, long windowSeconds) {
            return now - windowStart >= windowSeconds;
        }

        boolean incrementAndCheck(int max) {
            return count.incrementAndGet() <= max;
        }
    }
}
