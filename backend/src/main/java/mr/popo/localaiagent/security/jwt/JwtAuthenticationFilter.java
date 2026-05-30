package mr.popo.localaiagent.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jws;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mr.popo.localaiagent.security.userdetails.AppUserDetails;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader(HEADER);
        if (header != null && header.startsWith(PREFIX)) {
            String token = header.substring(PREFIX.length());
            try {
                Jws<Claims> jws = jwtService.parse(token);
                Claims claims = jws.getPayload();

                String type = claims.get("type", String.class);
                if (!JwtTokenType.ACCESS.name().equals(type)) {
                    log.debug("Rejected non-ACCESS token on protected endpoint");
                } else {
                    Long userId = Long.parseLong(claims.getSubject());
                    String username = claims.get("username", String.class);
                    @SuppressWarnings("unchecked")
                    List<String> roles = claims.get("roles", List.class);
                    if (roles == null) roles = List.of();

                    AppUserDetails principal = new AppUserDetails(userId, username, roles);
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                            principal, null, principal.getAuthorities());
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);

                    MDC.put("userId", String.valueOf(userId));
                }
            } catch (JwtException | IllegalArgumentException ex) {
                log.debug("JWT validation failed: {}", ex.getMessage());
                SecurityContextHolder.clearContext();
            }
        }

        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove("userId");
        }
    }
}
