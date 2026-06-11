package edu.fpt.sba301.bookstore.dto.response;

public class ApiResponse<T> {
    private int code;
    private String message;
    private T data;
}
