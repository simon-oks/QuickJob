package ro.oks.quickjob.auth;

import java.util.Map;
import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ro.oks.quickjob.auth.dto.AuthResponse;
import ro.oks.quickjob.auth.dto.RefreshTokenRequest;
import ro.oks.quickjob.auth.dto.RegisterRequest;
import ro.oks.quickjob.auth.dto.RequestOtpRequest;
import ro.oks.quickjob.auth.dto.VerifyOtpRequest;
import ro.oks.quickjob.common.i18n.MessageService;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentification", description = "Inscription, OTP, login, refresh token")
public class AuthController {

    private final AuthService authService;
    private final MessageService messageService;

    public AuthController(AuthService authService, MessageService messageService) {
        this.authService = authService;
        this.messageService = messageService;
    }

    @PostMapping("/otp/request")
    @Operation(summary = "Demander un code OTP par SMS ou email")
    public ResponseEntity<Map<String, String>> requestOtp(@Valid @RequestBody RequestOtpRequest request) {
        authService.requestOtp(request);
        return ResponseEntity.ok(Map.of("message", messageService.get("auth.otp_sent")));
    }

    @PostMapping("/register")
    @Operation(summary = "Créer un compte et s'authentifier (OTP requis au préalable)")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    @Operation(summary = "Se connecter avec numéro de téléphone ou email + code OTP")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody VerifyOtpRequest request) {
        AuthResponse response = authService.verifyOtpAndLogin(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Rafraîchir le token d'accès avec un refresh token")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refresh(request.refreshToken());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "Révoquer tous les refresh tokens de l'utilisateur")
    public ResponseEntity<Map<String, String>> logout(@AuthenticationPrincipal UUID userId) {
        authService.logout(userId);
        return ResponseEntity.ok(Map.of("message", messageService.get("auth.logout_success")));
    }
}
