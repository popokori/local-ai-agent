package mr.popo.localaiagent.common.exception;

import java.time.OffsetDateTime;
import java.util.List;

public record ApiError(
        OffsetDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldErrorDto> validationErrors
) {
    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(OffsetDateTime.now(), status, error, message, path, null);
    }

    public static ApiError ofValidation(int status, String message, String path, List<FieldErrorDto> errors) {
        return new ApiError(OffsetDateTime.now(), status, "Bad Request", message, path, errors);
    }

    public record FieldErrorDto(String field, String message) {}
}
