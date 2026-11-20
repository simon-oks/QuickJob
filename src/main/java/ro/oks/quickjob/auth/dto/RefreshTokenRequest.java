package ro.oks.quickjob.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
        @NotBlank(message = "{validation.auth.refresh_token_required}")
        String refreshToken
) {}
