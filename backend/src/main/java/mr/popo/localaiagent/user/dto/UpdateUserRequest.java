package mr.popo.localaiagent.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @Size(max = 128) String displayName,
        @Email @Size(max = 255) String email
) {}
