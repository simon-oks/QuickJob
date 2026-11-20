package ro.oks.quickjob.common.i18n;

import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import ro.oks.quickjob.user.User;
import ro.oks.quickjob.user.UserRepository;

@Component
public class MessageService {

    private static final Locale DEFAULT_LOCALE = Locale.FRENCH;

    private final MessageSource messageSource;
    private final UserRepository userRepository;

    public MessageService(MessageSource messageSource, UserRepository userRepository) {
        this.messageSource = messageSource;
        this.userRepository = userRepository;
    }

    public String get(String key, Object... args) {
        return messageSource.getMessage(key, args, key, resolveLocale());
    }

    public String get(String key, Locale locale, Object... args) {
        return messageSource.getMessage(key, args, key, locale);
    }

    private Locale resolveLocale() {
        // 1. Authenticated user: use their preferred language
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof java.util.UUID userId) {
            return userRepository.findById(userId)
                    .map(User::getPreferredLanguage)
                    .map(Locale::forLanguageTag)
                    .orElse(DEFAULT_LOCALE);
        }

        // 2. Accept-Language header (via LocaleContextHolder)
        Locale requestLocale = LocaleContextHolder.getLocale();
        if (requestLocale != null && isSupportedLocale(requestLocale)) {
            return requestLocale;
        }

        // 3. Default: French
        return DEFAULT_LOCALE;
    }

    private boolean isSupportedLocale(Locale locale) {
        String lang = locale.getLanguage();
        return "fr".equals(lang) || "en".equals(lang);
    }
}
