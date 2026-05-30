package mr.popo.localaiagent.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mr.popo.localaiagent.audit.domain.AuditAction;
import mr.popo.localaiagent.audit.service.AuditService;
import mr.popo.localaiagent.auth.domain.RefreshToken;
import mr.popo.localaiagent.auth.dto.LoginRequest;
import mr.popo.localaiagent.auth.dto.RegisterRequest;
import mr.popo.localaiagent.auth.dto.TokenResponse;
import mr.popo.localaiagent.auth.repository.RefreshTokenRepository;
import mr.popo.localaiagent.common.exception.BusinessException;
import mr.popo.localaiagent.security.jwt.JwtService;
import mr.popo.localaiagent.user.domain.Role;
import mr.popo.localaiagent.user.domain.User;
import mr.popo.localaiagent.user.repository.RoleRepository;
import mr.popo.localaiagent.user.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final AuditService auditService;
    private final SecureRandom random = new SecureRandom();

    @Transactional
    public TokenResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new BusinessException("Username already taken");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException("Email already taken");
        }

        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new IllegalStateException("USER role missing (V5 migration?)"));

        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setDisplayName(request.displayName());
        user.setRoles(Set.of(userRole));
        user = userRepository.saveAndFlush(user);

        auditService.log(AuditAction.REGISTER, user.getId(), null,
                Map.of("username", user.getUsername()), true, null);

        return issueTokens(user);
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        long start = System.currentTimeMillis();
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));

            User user = userRepository.findByUsername(authentication.getName())
                    .orElseThrow(() -> new IllegalStateException("Authenticated user vanished?"));

            TokenResponse response = issueTokens(user);
            auditService.log(AuditAction.LOGIN, user.getId(), null,
                    Map.of("username", user.getUsername()), true,
                    (int) (System.currentTimeMillis() - start));
            return response;
        } catch (org.springframework.security.core.AuthenticationException ex) {
            auditService.log(AuditAction.LOGIN_FAILED, null, null,
                    Map.of("username", request.username(), "reason", ex.getClass().getSimpleName()),
                    false, (int) (System.currentTimeMillis() - start));
            throw ex;
        }
    }

    @Transactional
    public TokenResponse refresh(String refreshToken) {
        String hash = sha256(refreshToken);
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new BusinessException("Invalid refresh token"));

        if (stored.isRevoked()) {
            // Token réutilisé après rotation → on révoque tout par sécurité.
            refreshTokenRepository.revokeAllByUserId(stored.getUserId());
            throw new BusinessException("Refresh token already used");
        }
        if (stored.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new BusinessException("Refresh token expired");
        }

        // Rotation : on révoque l'ancien et on émet une nouvelle paire.
        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        User user = userRepository.findById(stored.getUserId())
                .orElseThrow(() -> new BusinessException("User not found"));

        auditService.log(AuditAction.REFRESH, user.getId(), null, null, true, null);
        return issueTokens(user);
    }

    @Transactional
    public void logout(Long userId) {
        int n = refreshTokenRepository.revokeAllByUserId(userId);
        auditService.log(AuditAction.LOGOUT, userId, null,
                Map.of("revokedTokens", n), true, null);
    }

    private TokenResponse issueTokens(User user) {
        List<String> roles = user.getRoles().stream().map(Role::getName).toList();
        String access = jwtService.issueAccessToken(user.getId(), user.getUsername(), roles);
        String refresh = generateRefreshToken();

        RefreshToken entity = new RefreshToken();
        entity.setUserId(user.getId());
        entity.setTokenHash(sha256(refresh));
        entity.setExpiresAt(OffsetDateTime.now(ZoneOffset.UTC)
                .plusSeconds(jwtService.getRefreshTtlSeconds()));
        refreshTokenRepository.save(entity);

        return TokenResponse.bearer(access, refresh, jwtService.getAccessTtlSeconds());
    }

    private String generateRefreshToken() {
        byte[] bytes = new byte[48];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(s.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
