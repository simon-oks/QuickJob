package ro.oks.quickjob.mission;

import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ro.oks.quickjob.mission.dto.CreateMissionRequest;
import ro.oks.quickjob.mission.dto.MissionResponse;
import ro.oks.quickjob.mission.dto.NearbyMissionResponse;

@RestController
@RequestMapping("/api/v1/missions")
@Tag(name = "Missions", description = "Gestion des missions")
public class MissionController {

    private final MissionService missionService;
    private final MissionMapper missionMapper;

    public MissionController(MissionService missionService, MissionMapper missionMapper) {
        this.missionService = missionService;
        this.missionMapper = missionMapper;
    }

    @GetMapping("/categories")
    @Operation(summary = "Liste des catégories de mission")
    public MissionCategory[] getCategories() {
        return MissionCategory.values();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Créer une mission", description = "Publie une nouvelle mission. Réservé au rôle CLIENT avec téléphone vérifié.")
    public MissionResponse create(@Valid @RequestBody CreateMissionRequest request,
                                   @AuthenticationPrincipal UUID clientId) {
        Mission mission = missionService.create(request, clientId);
        return missionMapper.toResponse(mission);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'une mission",
               description = "Les coordonnées exactes ne sont visibles que par le client propriétaire ou les workers attribués.")
    public MissionResponse getById(@PathVariable UUID id,
                                    @AuthenticationPrincipal UUID userId) {
        Mission mission = missionService.getById(id);
        boolean canSeeLocation = mission.getClientId().equals(userId)
                || mission.hasWorker(userId);
        return missionMapper.toResponse(mission, canSeeLocation, userId);
    }

    @PostMapping("/{id}/start")
    @Operation(summary = "Démarrer une mission",
               description = "Passe la mission de ASSIGNED à IN_PROGRESS. Réservé au client propriétaire ou à un worker attribué.")
    public MissionResponse start(@PathVariable UUID id,
                                  @AuthenticationPrincipal UUID userId) {
        Mission mission = missionService.start(id, userId);
        return missionMapper.toResponse(mission, userId);
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "Terminer une mission",
               description = "Passe la mission de IN_PROGRESS à COMPLETED. Réservé au client propriétaire ou à un worker attribué.")
    public MissionResponse complete(@PathVariable UUID id,
                                     @AuthenticationPrincipal UUID userId) {
        Mission mission = missionService.complete(id, userId);
        return missionMapper.toResponse(mission, userId);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Annuler une mission",
               description = "Annule la mission. Réservé au client propriétaire.")
    public MissionResponse cancel(@PathVariable UUID id,
                                   @AuthenticationPrincipal UUID clientId) {
        Mission mission = missionService.cancel(id, clientId);
        return missionMapper.toResponse(mission, clientId);
    }

    @GetMapping("/me/client")
    @Operation(summary = "Mes missions en tant que client",
               description = "Liste paginée des missions créées par l'utilisateur connecté.")
    public Page<MissionResponse> getMyMissionsAsClient(@AuthenticationPrincipal UUID clientId,
                                                        Pageable pageable) {
        return missionService.findByClientId(clientId, pageable)
                .map(m -> missionMapper.toResponse(m, clientId));
    }

    @GetMapping("/me/worker")
    @Operation(summary = "Mes missions en tant que worker",
               description = "Liste paginée des missions auxquelles l'utilisateur est attribué.")
    public Page<MissionResponse> getMyMissionsAsWorker(@AuthenticationPrincipal UUID workerId,
                                                        Pageable pageable) {
        return missionService.findByWorkerId(workerId, pageable)
                .map(m -> missionMapper.toResponse(m, workerId));
    }

    @GetMapping("/feed")
    @Operation(summary = "Flux Explorer personnalise",
               description = "Missions publiees filtrees par les categories preferees du chercheur. "
                       + "Triees par recence. Exclut les missions deja candidatees. "
                       + "Fournir lat/lon pour calculer la distance.")
    public Page<NearbyMissionResponse> getFeed(
            @AuthenticationPrincipal UUID workerId,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lon,
            Pageable pageable) {
        Page<Mission> missions = missionService.findFeed(workerId, pageable);
        return missions.map(mission -> {
            double distance = 0;
            if (lat != null && lon != null) {
                distance = missionService.distanceTo(mission.getId(), lat, lon);
            }
            return missionMapper.toNearbyResponse(mission, distance);
        });
    }

    @GetMapping("/nearby")
    @Operation(summary = "Recherche de missions à proximité",
               description = "Retourne les missions publiées dans un rayon donné, en excluant celles auxquelles le worker a déjà postulé. Rayon par défaut : 5 km, maximum : 20 km.")
    public Page<NearbyMissionResponse> findNearby(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam(required = false) Double radius,
            @AuthenticationPrincipal UUID workerId,
            Pageable pageable) {
        Page<Mission> missions = missionService.findNearby(lat, lon, radius, workerId, pageable);
        return missions.map(m -> {
            double distance = missionService.distanceTo(m.getId(), lat, lon);
            return missionMapper.toNearbyResponse(m, distance);
        });
    }
}
