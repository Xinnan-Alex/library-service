package com.alexleong.libraryservice.error;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;

@Schema(description = "Stable JSON error response")
public record ApiError(
        Instant timestamp,
        @Schema(example = "400") int status,
        @Schema(example = "Bad Request") String error,
        String message,
        @Schema(example = "/api/v1/borrowers") String path,
        @Schema(description = "Validation messages keyed by request field; empty for non-field errors")
        Map<String, String> fieldErrors) {
}