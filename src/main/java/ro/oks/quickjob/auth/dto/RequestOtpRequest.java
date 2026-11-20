package ro.oks.quickjob.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RequestOtpRequest(
        @NotBlank(message = "{validation.auth.phone_required}")
        @Pattern(regexp = "^\\+\\d{10,15}$", message = "{validation.auth.phone_format}")
        String phoneNumber
) {}
