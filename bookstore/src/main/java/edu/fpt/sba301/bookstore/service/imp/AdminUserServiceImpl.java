package edu.fpt.sba301.bookstore.service.imp;

import edu.fpt.sba301.bookstore.dto.request.UpdateAdminUserRequest;
import edu.fpt.sba301.bookstore.dto.response.AdminUserResponse;
import edu.fpt.sba301.bookstore.dto.response.PageResponse;
import edu.fpt.sba301.bookstore.entity.User;
import edu.fpt.sba301.bookstore.repository.UserRepository;
import edu.fpt.sba301.bookstore.service.AdminUserService;
import edu.fpt.sba301.bookstore.service.RefreshTokenService;
import edu.fpt.sba301.bookstore.support.PaginationSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminUserResponse> listUsers(int page, int size) {
        Page<AdminUserResponse> users = userRepository
                .findAllByOrderByCreatedAtDesc(PaginationSupport.pageRequest(page, size))
                .map(this::toResponse);
        return PageResponse.from(users);
    }

    @Override
    @Transactional
    public AdminUserResponse updateUser(User admin, Long userId, UpdateAdminUserRequest request) {
        if (admin.getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot modify your own account");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        boolean disabling = request.enabled() != null && Boolean.FALSE.equals(request.enabled());
        boolean roleChanging = request.role() != null && !request.role().equals(user.getRole());

        if (roleChanging) {
            user.setRole(request.role());
        }
        if (request.enabled() != null) {
            user.setEnabled(request.enabled());
        }

        user = userRepository.save(user);

        if (disabling) {
            refreshTokenService.revokeAllForUser(user);
        }

        return toResponse(user);
    }

    private AdminUserResponse toResponse(User user) {
        return new AdminUserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.getEnabled(),
                user.getCreatedAt());
    }
}
