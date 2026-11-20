package ro.oks.quickjob.application.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateApplicationRequest(
        @NotNull(message = "{validation.application.mission_id_required}")
        UUID missionId,

        @Size(max = 500, message = "{validation.application.message_max_size}")
        String message
) {
}
