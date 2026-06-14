package edu.fpt.sba301.bookstore.dto.response;

import lombok.Data;

@Data
public class ApiResponse<T> {
    private int code;
    private String message;
    private T data;
}
