package ro.oks.quickjob.common.ratelimit;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filtre HTTP qui limite le débit sur les endpoints sensibles (OTP, register, login).
 * 5 requêtes par minute par IP sur ces endpoints.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimiter otpLimiter = new RateLimiter(5, 60);
    private final RateLimiter authLimiter = new RateLimiter(10, 60);
    private final RateLimiter applicationLimiter = new RateLimiter(20, 60);
    private final MessageSource messageSource;

    public RateLimitFilter(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        String ip = getClientIp(request);

        if (path.startsWith("/api/v1/auth/otp")) {
            if (!otpLimiter.tryAcquire(ip)) {
                sendTooManyRequests(response);
                return;
            }
        } else if (path.startsWith("/api/v1/auth/register") || path.startsWith("/api/v1/auth/login")) {
            if (!authLimiter.tryAcquire(ip)) {
                sendTooManyRequests(response);
                return;
            }
        } else if ("POST".equalsIgnoreCase(request.getMethod())
                && path.equals("/api/v1/applications")) {
            if (!applicationLimiter.tryAcquire(ip)) {
                sendTooManyRequests(response);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private void sendTooManyRequests(HttpServletResponse response) throws IOException {
        String detail = messageSource.getMessage("rate_limit.exceeded", null, LocaleContextHolder.getLocale());
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(
                "{\"type\":\"about:blank\",\"title\":\"Too Many Requests\",\"status\":429,\"detail\":\""
                        + detail.replace("\"", "\\\"")
                        + "\",\"code\":\"RATE_LIMIT_EXCEEDED\"}");
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
