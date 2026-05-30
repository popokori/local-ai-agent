package mr.popo.localaiagent.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mr.popo.localaiagent.auth.dto.LoginRequest;
import mr.popo.localaiagent.auth.dto.RefreshRequest;
import mr.popo.localaiagent.auth.dto.RegisterRequest;
import mr.popo.localaiagent.auth.dto.TokenResponse;
import mr.popo.localaiagent.auth.service.AuthService;
import mr.popo.localaiagent.security.userdetails.AppUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "Authentification et gestion des tokens JWT")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Inscription d'un nouvel utilisateur")
    @PostMapping("/register")
    public ResponseEntity<TokenResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @Operation(summary = "Login (renvoie access + refresh tokens)")
    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @Operation(summary = "Rotation du refresh token")
    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request.refreshToken());
    }

    @Operation(summary = "Logout (révoque tous les refresh tokens actifs)",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@AuthenticationPrincipal AppUserDetails principal) {
        authService.logout(principal.getId());
    }
}
