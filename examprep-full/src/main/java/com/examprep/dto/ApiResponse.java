package com.examprep.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Uniform API response wrapper used by all controllers.
 *
 * Success:  { "success": true,  "message": "...", "data": <T> }
 * Error:    { "success": false, "message": "..." }
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;
    private String  message;
    private T       data;

    private ApiResponse(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data    = data;
    }

    // ── Factory helpers ───────────────────────────────────────────

    /** Success response with message and data. */
    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }

    /** Success response with data only (message defaults to "OK"). */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "OK", data);
    }

    /** Error response with message only. */
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null);
    }

    // ── Getters (required by Jackson) ─────────────────────────────

    public boolean isSuccess() { return success; }
    public String  getMessage() { return message; }
    public T       getData()    { return data; }
}
