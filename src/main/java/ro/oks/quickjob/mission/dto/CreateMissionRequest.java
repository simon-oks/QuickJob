package ro.oks.quickjob.mission.dto;

import java.time.OffsetDateTime;
import java.util.Set;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import ro.oks.quickjob.mission.MissionCategory;

public record CreateMissionRequest(
        @NotNull(message = "{validation.mission.category_required}")
        MissionCategory category,

        @NotBlank(message = "{validation.mission.title_required}")
        @Size(max = 100, message = "{validation.mission.title_max_size}")
        String title,

        @Size(max = 1000, message = "{validation.mission.description_max_size}")
        String description,

        @NotNull(message = "{validation.mission.latitude_required}")
        @DecimalMin(value = "-90", message = "{validation.mission.latitude_range}")
        @DecimalMax(value = "90", message = "{validation.mission.latitude_range}")
        Double latitude,

        @NotNull(message = "{validation.mission.longitude_required}")
        @DecimalMin(value = "-180", message = "{validation.mission.longitude_range}")
        @DecimalMax(value = "180", message = "{validation.mission.longitude_range}")
        Double longitude,

        @NotBlank(message = "{validation.mission.address_label_required}")
        @Size(max = 255, message = "{validation.mission.address_label_max_size}")
        String addressLabel,

        @NotNull(message = "{validation.mission.start_at_required}")
        OffsetDateTime startAt,

        @NotNull(message = "{validation.mission.duration_required}")
        @Positive(message = "{validation.mission.duration_positive}")
        Integer durationMinutes,

        @NotNull(message = "{validation.mission.amount_required}")
        @Positive(message = "{validation.mission.amount_positive}")
        Long amountMinor,

        @NotBlank(message = "{validation.mission.currency_required}")
        @Size(min = 3, max = 3, message = "{validation.mission.currency_format}")
        String currency,

        @Positive(message = "{validation.mission.required_workers_positive}")
        Integer requiredWorkers,

        @Positive(message = "{validation.mission.notification_radius_positive}")
        Integer notificationRadiusMeters,

        Set<MissionCategory> targetWorkerCategories,

        @DecimalMin(value = "-90", message = "{validation.mission.delivery_latitude_range}")
        @DecimalMax(value = "90", message = "{validation.mission.delivery_latitude_range}")
        Double deliveryLatitude,

        @DecimalMin(value = "-180", message = "{validation.mission.delivery_longitude_range}")
        @DecimalMax(value = "180", message = "{validation.mission.delivery_longitude_range}")
        Double deliveryLongitude,

        @Size(max = 255, message = "{validation.mission.delivery_address_label_max_size}")
        String deliveryAddressLabel,

        @Size(max = 2000, message = "{validation.mission.additional_info_max_size}")
        String additionalInfo
) {
}
