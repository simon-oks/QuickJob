package ro.oks.quickjob.auth;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.oks.quickjob.auth.dto.AuthResponse;
import ro.oks.quickjob.auth.dto.RegisterRequest;
import ro.oks.quickjob.auth.dto.RequestOtpRequest;
import ro.oks.quickjob.auth.dto.VerifyOtpRequest;
import ro.oks.quickjob.common.error.BusinessException;
import ro.oks.quickjob.common.error.ErrorCode;
import ro.oks.quickjob.common.messaging.MessageSenderRegistry;
import ro.oks.quickjob.user.User;
import ro.oks.quickjob.user.UserRepository;

@Service
@Transactional(readOnly = true)
public class AuthService {

    private final OtpService otpService;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final MessageSenderRegistry messageSenderRegistry;

    public AuthService(OtpService otpService, JwtService jwtService,
                       UserRepository userRepository, MessageSenderRegistry messageSenderRegistry) {
        this.otpService = otpService;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.messageSenderRegistry = messageSenderRegistry;
    }

    public void requestOtp(RequestOtpRequest request) {
        IdentifierInfo info = resolveIdentifier(request.phoneNumber(), request.email());
        otpService.generateAndSend(info.identifier(), info.channel(), Locale.FRENCH);
    }

    @Transactional
    public AuthResponse verifyOtpAndLogin(VerifyOtpRequest request) {
        IdentifierInfo info = resolveIdentifier(request.phoneNumber(), request.email());

        boolean valid = otpService.verify(info.identifier(), request.otpCode());
        if (!valid) {
            throw new BusinessException(
                    ErrorCode.AUTH_INVALID_OTP,
                    "auth.invalid_otp",
                    HttpStatus.UNAUTHORIZED);
        }

        Optional<User> existingUser = findUserByIdentifier(info);
        if (existingUser.isEmpty()) {
            throw new BusinessException(
                    ErrorCode.AUTH_USER_NOT_REGISTERED,
                    "auth.user_not_registered",
                    HttpStatus.NOT_FOUND);
        }

        User user = existingUser.get();
        if ("sms".equals(info.channel()) && !user.isPhoneVerified()) {
            user.setPhoneVerified(true);
        }
        if ("email".equals(info.channel()) && !user.isEmailVerified()) {
            user.setEmailVerified(true);
        }

        return jwtService.generateTokenPair(user);
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        boolean hasPhone = request.phoneNumber() != null && !request.phoneNumber().isBlank();
        boolean hasEmail = request.email() != null && !request.email().isBlank();

        if (!hasPhone && !hasEmail) {
            throw new BusinessException(
                    ErrorCode.AUTH_IDENTIFIER_REQUIRED,
                    "auth.identifier_required",
                    HttpStatus.BAD_REQUEST);
        }

        // L'OTP a été envoyé sur un seul canal : on tente les deux pour trouver le bon
        boolean valid = false;
        String verifiedChannel = null;
        if (hasPhone && otpService.verify(request.phoneNumber(), request.otpCode())) {
            valid = true;
            verifiedChannel = "sms";
        }
        if (!valid && hasEmail && otpService.verify(request.email(), request.otpCode())) {
            valid = true;
            verifiedChannel = "email";
        }
        if (!valid) {
            throw new BusinessException(
                    ErrorCode.AUTH_INVALID_OTP,
                    "auth.invalid_otp",
                    HttpStatus.UNAUTHORIZED);
        }

        // Vérifier l'unicité du téléphone et de l'email
        if (hasPhone && userRepository.findByPhoneNumber(request.phoneNumber()).isPresent()) {
            throw new BusinessException(
                    ErrorCode.AUTH_PHONE_ALREADY_REGISTERED,
                    "auth.phone_already_registered",
                    HttpStatus.CONFLICT);
        }
        if (hasEmail && userRepository.findByEmail(request.email()).isPresent()) {
            throw new BusinessException(
                    ErrorCode.AUTH_EMAIL_ALREADY_REGISTERED,
                    "auth.email_already_registered",
                    HttpStatus.CONFLICT);
        }

        User user = new User(UUID.randomUUID(), request.phoneNumber(), request.displayName());
        if (hasPhone) {
            user.setPhoneVerified("sms".equals(verifiedChannel));
        }
        if (hasEmail) {
            user.setEmail(request.email());
            user.setEmailVerified("email".equals(verifiedChannel));
        }
        if (request.preferredLanguage() != null) {
            user.setPreferredLanguage(request.preferredLanguage());
        }
        userRepository.save(user);

        Locale locale = Locale.forLanguageTag(user.getPreferredLanguage());
        messageSenderRegistry.getSender(verifiedChannel)
                .sendWelcome("sms".equals(verifiedChannel) ? request.phoneNumber() : request.email(),
                        user.getDisplayName(), locale);

        return jwtService.generateTokenPair(user);
    }

    @Transactional
    public AuthResponse refresh(String rawRefreshToken) {
        UUID userId = jwtService.getUserIdFromRefreshToken(rawRefreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.USER_NOT_FOUND,
                        "user.not_found",
                        HttpStatus.NOT_FOUND));
        return jwtService.refreshAccessToken(rawRefreshToken, user);
    }

    @Transactional
    public void logout(UUID userId) {
        jwtService.revokeAllTokens(userId);
    }

    private IdentifierInfo resolveIdentifier(String phoneNumber, String email) {
        boolean hasPhone = phoneNumber != null && !phoneNumber.isBlank();
        boolean hasEmail = email != null && !email.isBlank();

        if (hasPhone && hasEmail) {
            throw new BusinessException(
                    ErrorCode.AUTH_IDENTIFIER_REQUIRED,
                    "auth.identifier_required",
                    HttpStatus.BAD_REQUEST);
        }
        if (!hasPhone && !hasEmail) {
            throw new BusinessException(
                    ErrorCode.AUTH_IDENTIFIER_REQUIRED,
                    "auth.identifier_required",
                    HttpStatus.BAD_REQUEST);
        }
        if (hasPhone) {
            return new IdentifierInfo(phoneNumber, "sms");
        }
        return new IdentifierInfo(email, "email");
    }

    private Optional<User> findUserByIdentifier(IdentifierInfo info) {
        if ("sms".equals(info.channel())) {
            return userRepository.findByPhoneNumber(info.identifier());
        }
        return userRepository.findByEmail(info.identifier());
    }

    private record IdentifierInfo(String identifier, String channel) {}
}
