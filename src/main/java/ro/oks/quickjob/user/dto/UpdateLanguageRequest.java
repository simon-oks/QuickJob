package ro.oks.quickjob.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateLanguageRequest(
        @NotBlank(message = "{validation.auth.language_format}")
        @Pattern(regexp = "^(fr|en)$", message = "{validation.auth.language_format}")
        String language
) {
}
