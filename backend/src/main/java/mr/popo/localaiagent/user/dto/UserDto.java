package mr.popo.localaiagent.user.dto;

import java.time.OffsetDateTime;
import java.util.Set;

public record UserDto(
        Long id,
        String username,
        String email,
        String displayName,
        boolean enabled,
        Set<String> roles,
        OffsetDateTime createdAt
) {}
