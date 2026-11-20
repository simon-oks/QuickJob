package ro.oks.quickjob.application;

import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ro.oks.quickjob.application.dto.ApplicationResponse;
import ro.oks.quickjob.application.dto.CreateApplicationRequest;

@RestController
@RequestMapping("/api/v1/applications")
@Tag(name = "Applications", description = "Candidatures aux missions")
public class ApplicationController {

    private final ApplicationService applicationService;
    private final ApplicationMapper applicationMapper;

    public ApplicationController(ApplicationService applicationService,
                                  ApplicationMapper applicationMapper) {
        this.applicationService = applicationService;
        this.applicationMapper = applicationMapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Postuler à une mission")
    public ApplicationResponse apply(@Valid @RequestBody CreateApplicationRequest request,
                                      @AuthenticationPrincipal UUID workerId) {
        Application application = applicationService.apply(request, workerId);
        return applicationMapper.toResponse(application);
    }

    @PostMapping("/{id}/accept")
    @Operation(summary = "Accepter une candidature",
               description = "Réservé au propriétaire de la mission. Attribue le worker à la mission. Si la mission atteint le nombre de workers requis, les autres candidatures en attente sont automatiquement rejetées.")
    public ApplicationResponse accept(@PathVariable UUID id,
                                       @AuthenticationPrincipal UUID clientId) {
        Application application = applicationService.accept(id, clientId);
        return applicationMapper.toResponse(application);
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Rejeter une candidature", description = "Réservé au propriétaire de la mission.")
    public ApplicationResponse reject(@PathVariable UUID id,
                                       @AuthenticationPrincipal UUID clientId) {
        Application application = applicationService.reject(id, clientId);
        return applicationMapper.toResponse(application);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Annuler sa candidature", description = "Le worker annule sa propre candidature.")
    public ApplicationResponse cancel(@PathVariable UUID id,
                                       @AuthenticationPrincipal UUID workerId) {
        Application application = applicationService.cancel(id, workerId);
        return applicationMapper.toResponse(application);
    }

    @GetMapping("/mission/{missionId}")
    @Operation(summary = "Lister les candidatures d'une mission", description = "Réservé au propriétaire de la mission.")
    public Page<ApplicationResponse> getByMission(@PathVariable UUID missionId,
                                                    @AuthenticationPrincipal UUID clientId,
                                                    Pageable pageable) {
        return applicationService.getByMissionId(missionId, clientId, pageable)
                .map(applicationMapper::toResponse);
    }

    @GetMapping("/me")
    @Operation(summary = "Lister mes candidatures")
    public Page<ApplicationResponse> getMyApplications(@AuthenticationPrincipal UUID workerId,
                                                        Pageable pageable) {
        return applicationService.getByWorkerId(workerId, pageable)
                .map(applicationMapper::toResponse);
    }
}
