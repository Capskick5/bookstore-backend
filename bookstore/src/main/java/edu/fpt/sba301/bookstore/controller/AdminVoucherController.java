package edu.fpt.sba301.bookstore.controller;

import edu.fpt.sba301.bookstore.dto.request.UpdateVoucherActiveRequest;
import edu.fpt.sba301.bookstore.dto.request.VoucherRequest;
import edu.fpt.sba301.bookstore.dto.response.ApiResponse;
import edu.fpt.sba301.bookstore.dto.response.VoucherResponse;
import edu.fpt.sba301.bookstore.entity.Voucher;
import edu.fpt.sba301.bookstore.repository.VoucherRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/admin/vouchers")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminVoucherController {

    private final VoucherRepository voucherRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<VoucherResponse>>> getVouchers() {
        List<VoucherResponse> data = voucherRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
        return ResponseEntity.ok(response(200, "OK", data));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<VoucherResponse>> createVoucher(
            @Valid @RequestBody VoucherRequest request) {
        String code = request.code().trim().toUpperCase();
        if (voucherRepository.findByCodeIgnoreCase(code).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Voucher code already exists");
        }
        validateVoucherRequest(request);

        Voucher voucher = new Voucher();
        voucher.setCode(code);
        voucher.setType(request.type());
        voucher.setValue(request.value());
        voucher.setMinOrder(request.minOrder());
        voucher.setMaxDiscount(request.maxDiscount());
        voucher.setUsageLimit(request.usageLimit());
        voucher.setPerUserLimit(request.perUserLimit());
        voucher.setStartsAt(request.startsAt());
        voucher.setEndsAt(request.endsAt());
        voucher.setActive(request.active() == null || request.active());
        voucher.setUsedCount(0);

        Voucher saved = voucherRepository.save(voucher);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response(201, "Created", mapToResponse(saved)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<VoucherResponse>> updateVoucher(
            @PathVariable Long id,
            @Valid @RequestBody VoucherRequest request) {
        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Voucher not found"));

        String code = request.code().trim().toUpperCase();
        voucherRepository.findByCodeIgnoreCase(code).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Voucher code already exists");
            }
        });
        validateVoucherRequest(request);

        voucher.setCode(code);
        voucher.setType(request.type());
        voucher.setValue(request.value());
        voucher.setMinOrder(request.minOrder());
        voucher.setMaxDiscount(request.maxDiscount());
        voucher.setUsageLimit(request.usageLimit());
        voucher.setPerUserLimit(request.perUserLimit());
        voucher.setStartsAt(request.startsAt());
        voucher.setEndsAt(request.endsAt());
        if (request.active() != null) {
            voucher.setActive(request.active());
        }

        Voucher saved = voucherRepository.save(voucher);
        return ResponseEntity.ok(response(200, "OK", mapToResponse(saved)));
    }

    @PatchMapping("/{id}/active")
    public ResponseEntity<ApiResponse<VoucherResponse>> updateVoucherActive(
            @PathVariable Long id,
            @Valid @RequestBody UpdateVoucherActiveRequest request) {
        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Voucher not found"));

        voucher.setActive(request.active());
        Voucher saved = voucherRepository.save(voucher);
        return ResponseEntity.ok(response(200, "OK", mapToResponse(saved)));
    }

    private void validateVoucherRequest(VoucherRequest request) {
        if ("PERCENT".equals(request.type()) && request.value() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Percent voucher cannot exceed 100");
        }
        if (request.startsAt() != null && request.endsAt() != null
                && !request.endsAt().isAfter(request.startsAt())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Voucher end time must be after start time");
        }
    }

    private VoucherResponse mapToResponse(Voucher voucher) {
        return new VoucherResponse(
                voucher.getId(), voucher.getCode(), voucher.getType(), voucher.getValue(),
                voucher.getMinOrder(), voucher.getMaxDiscount(), voucher.getUsageLimit(),
                voucher.getUsedCount(), voucher.getPerUserLimit(), voucher.getStartsAt(),
                voucher.getEndsAt(), voucher.getActive());
    }

    private <T> ApiResponse<T> response(int code, String message, T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(code);
        response.setMessage(message);
        response.setData(data);
        return response;
    }
}
