package ro.oks.quickjob.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RequestOtpRequest(
        @Pattern(regexp = "^\\+\\d{10,15}$", message = "{validation.auth.phone_format}")
        String phoneNumber,

        @Email(message = "{validation.auth.email_format}")
        @Size(max = 255, message = "{validation.auth.email_max_size}")
        String email
) {}
