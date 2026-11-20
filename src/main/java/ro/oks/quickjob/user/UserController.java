package ro.oks.quickjob.user;

import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ro.oks.quickjob.user.dto.UpdateCategoriesRequest;
import ro.oks.quickjob.user.dto.UpdateLanguageRequest;
import ro.oks.quickjob.user.dto.UpdateLocationRequest;
import ro.oks.quickjob.user.dto.UserResponse;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Utilisateurs", description = "Profil utilisateur")
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;

    public UserController(UserService userService, UserMapper userMapper) {
        this.userService = userService;
        this.userMapper = userMapper;
    }

    @GetMapping("/me")
    @Operation(summary = "Mon profil", description = "Retourne les informations du profil de l'utilisateur connecté.")
    public UserResponse getMyProfile(@AuthenticationPrincipal UUID userId) {
        User user = userService.getById(userId);
        return userMapper.toResponse(user);
    }

    @PutMapping("/me/location")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Mettre à jour ma position",
               description = "Met à jour la dernière position connue du worker. Utilisé pour le ciblage des notifications de nouvelles missions.")
    public void updateLocation(@AuthenticationPrincipal UUID userId,
                               @Valid @RequestBody UpdateLocationRequest request) {
        userService.updateLocation(userId, request.latitude(), request.longitude());
    }

    @PutMapping("/me/categories")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Mettre à jour mes catégories",
               description = "Définit les catégories de missions pour lesquelles le worker est disponible. Un ensemble vide signifie polyvalent (toutes catégories).")
    public void updateCategories(@AuthenticationPrincipal UUID userId,
                                 @Valid @RequestBody UpdateCategoriesRequest request) {
        userService.updateCategories(userId, request.categories());
    }

    @PutMapping("/me/language")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Changer ma langue préférée",
               description = "Met à jour la langue préférée de l'utilisateur. Langues supportées : fr, en.")
    public void updateLanguage(@AuthenticationPrincipal UUID userId,
                               @Valid @RequestBody UpdateLanguageRequest request) {
        userService.updateLanguage(userId, request.language());
    }
}
