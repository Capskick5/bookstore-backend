package edu.fpt.sba301.bookstore.support;

import edu.fpt.sba301.bookstore.dto.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public final class ApiResponseSupport {

    private ApiResponseSupport() {
    }

    public static <T> ResponseEntity<ApiResponse<T>> ok(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(HttpStatus.OK.value());
        response.setMessage("OK");
        response.setData(data);
        return ResponseEntity.ok(response);
    }

    public static <T> ResponseEntity<ApiResponse<T>> created(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(HttpStatus.CREATED.value());
        response.setMessage("Created");
        response.setData(data);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    public static <T> ApiResponse<T> envelope(int code, String message, T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(code);
        response.setMessage(message);
        response.setData(data);
        return response;
    }
}
