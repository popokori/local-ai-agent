package mr.popo.localaiagent.auth.dto;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        long expiresIn,
        String tokenType
) {
    public static TokenResponse bearer(String access, String refresh, long expiresIn) {
        return new TokenResponse(access, refresh, expiresIn, "Bearer");
    }
}
