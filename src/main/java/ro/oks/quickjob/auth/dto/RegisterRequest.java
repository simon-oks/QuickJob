package ro.oks.quickjob.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "{validation.auth.phone_required}")
        @Pattern(regexp = "^\\+\\d{10,15}$", message = "{validation.auth.phone_format}")
        String phoneNumber,

        @NotBlank(message = "{validation.auth.display_name_required}")
        @Size(min = 2, max = 100, message = "{validation.auth.display_name_size}")
        String displayName,

        @NotBlank(message = "{validation.auth.otp_required}")
        @Pattern(regexp = "^\\d{6}$", message = "{validation.auth.otp_format}")
        String otpCode,

        @Pattern(regexp = "^(fr|en)$", message = "{validation.auth.language_format}")
        String preferredLanguage
) {}
