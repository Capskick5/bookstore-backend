package edu.fpt.sba301.bookstore.service;

import edu.fpt.sba301.bookstore.dto.request.UpdateAdminUserRequest;
import edu.fpt.sba301.bookstore.dto.response.AdminUserResponse;
import edu.fpt.sba301.bookstore.dto.response.PageResponse;
import edu.fpt.sba301.bookstore.entity.User;

public interface AdminUserService {
    PageResponse<AdminUserResponse> listUsers(int page, int size);

    AdminUserResponse updateUser(User admin, Long userId, UpdateAdminUserRequest request);
}
