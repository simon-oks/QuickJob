package ro.oks.quickjob.auth;

import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.oks.quickjob.auth.dto.AuthResponse;
import ro.oks.quickjob.auth.dto.RegisterRequest;
import ro.oks.quickjob.common.error.BusinessException;
import ro.oks.quickjob.common.error.ErrorCode;
import ro.oks.quickjob.user.User;
import ro.oks.quickjob.user.UserRepository;

@Service
@Transactional(readOnly = true)
public class AuthService {

    private final OtpService otpService;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    public AuthService(OtpService otpService, JwtService jwtService, UserRepository userRepository) {
        this.otpService = otpService;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    public void requestOtp(String phoneNumber) {
        otpService.generateAndSend(phoneNumber);
    }

    @Transactional
    public AuthResponse verifyOtpAndLogin(String phoneNumber, String otpCode) {
        boolean valid = otpService.verify(phoneNumber, otpCode);
        if (!valid) {
            throw new BusinessException(
                    ErrorCode.AUTH_INVALID_OTP,
                    "auth.invalid_otp",
                    HttpStatus.UNAUTHORIZED);
        }

        Optional<User> existingUser = userRepository.findByPhoneNumber(phoneNumber);
        if (existingUser.isEmpty()) {
            throw new BusinessException(
                    ErrorCode.AUTH_USER_NOT_REGISTERED,
                    "auth.user_not_registered",
                    HttpStatus.NOT_FOUND);
        }

        User user = existingUser.get();
        if (!user.isPhoneVerified()) {
            user.setPhoneVerified(true);
        }

        return jwtService.generateTokenPair(user);
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        boolean valid = otpService.verify(request.phoneNumber(), request.otpCode());
        if (!valid) {
            throw new BusinessException(
                    ErrorCode.AUTH_INVALID_OTP,
                    "auth.invalid_otp",
                    HttpStatus.UNAUTHORIZED);
        }

        if (userRepository.findByPhoneNumber(request.phoneNumber()).isPresent()) {
            throw new BusinessException(
                    ErrorCode.AUTH_PHONE_ALREADY_REGISTERED,
                    "auth.phone_already_registered",
                    HttpStatus.CONFLICT);
        }

        User user = new User(UUID.randomUUID(), request.phoneNumber(), request.displayName());
        user.setPhoneVerified(true);
        if (request.preferredLanguage() != null) {
            user.setPreferredLanguage(request.preferredLanguage());
        }
        userRepository.save(user);

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
}
