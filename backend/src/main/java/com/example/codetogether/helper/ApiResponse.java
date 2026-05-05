package com.example.codetogether.helper;

import java.time.LocalDateTime;

public record ApiResponse<T>(
        boolean success,
        int status,
        String message,
        T data,
        String path,
        LocalDateTime timestamp
) {
    public static <T> ApiResponse<T> success(int status, String message, T data) {
        return new ApiResponse<>(true, status, message, data, null, LocalDateTime.now());
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return success(200, message, data);
    }

    public static ApiResponse<Void> error(int status, String message, String path) {
        return new ApiResponse<>(false, status, message, null, path, LocalDateTime.now());
    }
}
