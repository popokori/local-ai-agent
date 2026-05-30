package mr.popo.localaiagent.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mr.popo.localaiagent.security.userdetails.AppUserDetails;
import mr.popo.localaiagent.user.dto.ChangePasswordRequest;
import mr.popo.localaiagent.user.dto.UpdateUserRequest;
import mr.popo.localaiagent.user.dto.UserDto;
import mr.popo.localaiagent.user.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Users")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    @Operation(summary = "Profil de l'utilisateur courant")
    @GetMapping("/me")
    public UserDto getMe(@AuthenticationPrincipal AppUserDetails principal) {
        return userService.getMe(principal.getId());
    }

    @Operation(summary = "Mise à jour du profil")
    @PatchMapping("/me")
    public UserDto updateMe(@AuthenticationPrincipal AppUserDetails principal,
                            @Valid @RequestBody UpdateUserRequest request) {
        return userService.updateMe(principal.getId(), request);
    }

    @Operation(summary = "Changement de mot de passe")
    @PostMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@AuthenticationPrincipal AppUserDetails principal,
                               @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(principal.getId(), request);
    }
}
