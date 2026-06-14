package edu.fpt.sba301.bookstore.service;

import edu.fpt.sba301.bookstore.dto.response.LoginResponse;

public interface AuthService {
    LoginResponse login(String email, String password);
}
