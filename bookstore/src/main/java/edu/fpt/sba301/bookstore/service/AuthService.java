package edu.fpt.sba301.bookstore.service;

import edu.fpt.sba301.bookstore.dto.request.LoginRequest;
import edu.fpt.sba301.bookstore.dto.response.LoginResponse;

public interface AuthService {
    LoginResponse login(LoginRequest request);
}
