package ro.oks.quickjob.user.dto;

import java.util.Set;

import jakarta.validation.constraints.NotNull;
import ro.oks.quickjob.mission.MissionCategory;

public record UpdateCategoriesRequest(
        @NotNull Set<MissionCategory> categories
) {
}
