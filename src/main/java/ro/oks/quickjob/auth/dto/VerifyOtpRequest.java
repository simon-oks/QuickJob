package ro.oks.quickjob.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyOtpRequest(
        @Pattern(regexp = "^\\+\\d{10,15}$", message = "{validation.auth.phone_format}")
        String phoneNumber,

        @Email(message = "{validation.auth.email_format}")
        String email,

        @NotBlank(message = "{validation.auth.otp_required}")
        @Pattern(regexp = "^\\d{6}$", message = "{validation.auth.otp_format}")
        String otpCode
) {}
