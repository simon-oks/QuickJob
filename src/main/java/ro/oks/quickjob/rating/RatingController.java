package ro.oks.quickjob.rating;

import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ro.oks.quickjob.rating.dto.CreateRatingRequest;
import ro.oks.quickjob.rating.dto.RatingResponse;

@RestController
@RequestMapping("/api/v1/missions/{missionId}/ratings")
@Tag(name = "Ratings", description = "Notation mutuelle apres completion de mission")
public class RatingController {

    private final RatingService ratingService;
    private final RatingMapper ratingMapper;

    public RatingController(RatingService ratingService, RatingMapper ratingMapper) {
        this.ratingService = ratingService;
        this.ratingMapper = ratingMapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Noter une mission completee",
               description = "Le client note les chercheurs, les chercheurs notent le client. "
                       + "Fenetre de notation : 7 jours apres completion.")
    public RatingResponse createRating(
            @PathVariable UUID missionId,
            @Valid @RequestBody CreateRatingRequest request,
            @AuthenticationPrincipal UUID raterId) {
        Rating rating = ratingService.createRating(missionId, request, raterId);
        return ratingMapper.toResponse(rating);
    }
}
