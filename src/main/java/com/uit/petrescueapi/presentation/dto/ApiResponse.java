package com.uit.petrescueapi.presentation.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.MDC;

import java.time.LocalDateTime;

/**
 * Standard API response envelope used by every endpoint.
 *
 * <p>Success example:</p>
 * <pre>{
 *   "success": true,
 *   "status": 200,
 *   "message": "OK",
 *   "data": { ... },
 *   "timestamp": "2026-02-26T18:00:00",
 *   "correlationId": "abc-123"
 * }</pre>
 *
 * <p>Error example:</p>
 * <pre>{
 *   "success": false,
 *   "status": 404,
 *   "message": "Pet not found",
 *   "data": null,
 *   "timestamp": "2026-02-26T18:00:00",
 *   "correlationId": "abc-123"
 * }</pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standard API response wrapper")
public class ApiResponse<T> {

    @Schema(description = "Whether the request succeeded", example = "true")
    private boolean success;

    @Schema(description = "HTTP status code", example = "200")
    private int status;

    @Schema(description = "Human-readable message", example = "OK")
    private String message;

    @Schema(description = "Machine-readable error code, only on business errors", example = "PET_NOT_AVAILABLE")
    private String errorCode;

    @Schema(description = "Response payload")
    private T data;

    @Schema(description = "ISO-8601 timestamp", example = "2026-02-26T18:00:00")
    private String timestamp;

    @Schema(description = "Correlation ID for tracing", example = "550e8400-e29b-41d4-a716-446655440000")
    private String correlationId;

    // ── Static factories ─────────────────────────

    public static <T> ApiResponse<T> ok(T data) {
        return ok(data, "OK");
    }

    public static <T> ApiResponse<T> ok(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .status(200)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now().toString())
                .correlationId(MDC.get("correlationId"))
                .build();
    }

    public static <T> ApiResponse<T> created(T data) {
        return created(data, "Created");
    }

    public static <T> ApiResponse<T> created(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .status(201)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now().toString())
                .correlationId(MDC.get("correlationId"))
                .build();
    }

    public static <T> ApiResponse<T> noContent() {
        return ApiResponse.<T>builder()
                .success(true)
                .status(204)
                .message("No Content")
                .timestamp(LocalDateTime.now().toString())
                .correlationId(MDC.get("correlationId"))
                .build();
    }

    public static <T> ApiResponse<T> error(int status, String message, String errorCode) {
        ApiResponse<T> response = error(status, message);
        response.setErrorCode(errorCode);
        return response;
    }

    public static <T> ApiResponse<T> error(int status, String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .status(status)
                .message(message)
                .timestamp(LocalDateTime.now().toString())
                .correlationId(MDC.get("correlationId"))
                .build();
    }
}
