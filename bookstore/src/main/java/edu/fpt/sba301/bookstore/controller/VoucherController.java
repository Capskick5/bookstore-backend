package edu.fpt.sba301.bookstore.controller;

import edu.fpt.sba301.bookstore.dto.request.ApplyVoucherRequest;
import edu.fpt.sba301.bookstore.dto.response.ApiResponse;
import edu.fpt.sba301.bookstore.dto.response.ApplyVoucherResponse;
import edu.fpt.sba301.bookstore.dto.response.VoucherResponse;
import edu.fpt.sba301.bookstore.entity.User;
import edu.fpt.sba301.bookstore.service.VoucherService;
import edu.fpt.sba301.bookstore.support.CurrentUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Validated
public class VoucherController {

    private final VoucherService voucherService;
    private final CurrentUserService currentUserService;

    @PostMapping("/api/vouchers/apply")
    public ResponseEntity<ApiResponse<ApplyVoucherResponse>> applyVoucher(
            @Valid @RequestBody ApplyVoucherRequest request,
            Principal principal) {
        User user = currentUserService.requireUser(principal);
        ApplyVoucherResponse data = voucherService.applyVoucher(user, request);

        ApiResponse<ApplyVoucherResponse> response = new ApiResponse<>();
        response.setCode(200);
        response.setMessage("OK");
        response.setData(data);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/me/vouchers")
    public ResponseEntity<ApiResponse<List<VoucherResponse>>> listMyVouchers(Principal principal) {
        User user = currentUserService.requireUser(principal);
        List<VoucherResponse> data = voucherService.listApplicableVouchers(user);

        ApiResponse<List<VoucherResponse>> response = new ApiResponse<>();
        response.setCode(200);
        response.setMessage("OK");
        response.setData(data);
        return ResponseEntity.ok(response);
    }
}
