package edu.fpt.sba301.bookstore.controller;

import edu.fpt.sba301.bookstore.dto.response.ApiResponse;
import edu.fpt.sba301.bookstore.dto.response.PointsResponse;
import edu.fpt.sba301.bookstore.service.PointService;
import edu.fpt.sba301.bookstore.support.ApiResponseSupport;
import edu.fpt.sba301.bookstore.support.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/auth/me/points")
@RequiredArgsConstructor
public class PointsController {

    private final PointService pointService;
    private final CurrentUserService currentUserService;

    @GetMapping
    public ResponseEntity<ApiResponse<PointsResponse>> getPoints(
            Principal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PointsResponse data = pointService.getPointsHistory(currentUserService.requireUser(principal), page, size);
        return ApiResponseSupport.ok(data);
    }
}
