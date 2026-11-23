package com.notesapp.dto.common;

import java.time.Instant;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.HttpStatus;

/** The single error format returned by every failing request. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        @Schema(example = "2026-11-20T01:02:54.576Z") Instant timestamp,
        @Schema(example = "400") int status,
        @Schema(example = "Bad Request") String error,
        @Schema(example = "Validation failed") String message,
        @Schema(example = "/api/notes") String path,
        @Schema(description = "Field name -> problem (only for validation errors)",
                example = "{\"title\": \"must not be blank\"}")
        Map<String, String> validationErrors) {

    public static ErrorResponse of(HttpStatus status, String message, String path) {
        return new ErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(), message, path, null);
    }

    public static ErrorResponse validation(String path, Map<String, String> errors) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        return new ErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(),
                "Validation failed", path, errors);
    }
}
