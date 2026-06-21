package edu.fpt.sba301.bookstore.service.imp;

import edu.fpt.sba301.bookstore.constant.VoucherTypes;
import edu.fpt.sba301.bookstore.dto.request.ApplyVoucherRequest;
import edu.fpt.sba301.bookstore.dto.response.ApplyVoucherResponse;
import edu.fpt.sba301.bookstore.dto.response.VoucherResponse;
import edu.fpt.sba301.bookstore.entity.User;
import edu.fpt.sba301.bookstore.entity.Voucher;
import edu.fpt.sba301.bookstore.repository.VoucherRedemptionRepository;
import edu.fpt.sba301.bookstore.repository.VoucherRepository;
import edu.fpt.sba301.bookstore.service.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VoucherServiceImpl implements VoucherService {

    private final VoucherRepository voucherRepository;
    private final VoucherRedemptionRepository voucherRedemptionRepository;

    @Value("${app.order.shipping-fee:30000}")
    private long shippingFeeFlat;

    @Value("${app.order.free-shipping-threshold:300000}")
    private long freeShippingThreshold;

    @Override
    public ApplyVoucherResponse applyVoucher(User user, ApplyVoucherRequest request) {
        Voucher voucher = validateVoucher(request.code(), user, request.cartSubtotal());
        DiscountPreview preview = calculateDiscount(voucher, request.cartSubtotal());
        long discountedSubtotal = Math.max(0, request.cartSubtotal() - preview.amount());
        long shippingFee = calculateShippingFee(discountedSubtotal, preview.shipVoucher(), voucher);
        long total = discountedSubtotal + shippingFee;

        return new ApplyVoucherResponse(
                voucher.getCode(),
                voucher.getType(),
                preview.amount(),
                preview.shipVoucher() || shippingFee == 0,
                shippingFee,
                total,
                buildDescription(voucher, preview));
    }

    @Override
    public List<VoucherResponse> listApplicableVouchers(User user) {
        OffsetDateTime now = OffsetDateTime.now();
        return voucherRepository.findAll().stream()
                .filter(v -> Boolean.TRUE.equals(v.getActive()))
                .filter(v -> v.getStartsAt() == null || !v.getStartsAt().isAfter(now))
                .filter(v -> v.getEndsAt() == null || !v.getEndsAt().isBefore(now))
                .filter(v -> v.getUsageLimit() == null || v.getUsedCount() < v.getUsageLimit())
                .filter(v -> voucherRedemptionRepository.countByVoucherAndUser(v, user) < v.getPerUserLimit())
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public Voucher validateVoucher(String code, User user, long subtotal) {
        Voucher voucher = voucherRepository.findByCodeIgnoreCase(code.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid voucher code"));
        OffsetDateTime now = OffsetDateTime.now();
        if (Boolean.FALSE.equals(voucher.getActive())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Voucher is inactive");
        }
        if (voucher.getStartsAt() != null && voucher.getStartsAt().isAfter(now)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Voucher is not yet active");
        }
        if (voucher.getEndsAt() != null && voucher.getEndsAt().isBefore(now)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Voucher has expired");
        }
        if (subtotal < voucher.getMinOrder()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order subtotal below voucher minimum");
        }
        long userUsage = voucherRedemptionRepository.countByVoucherAndUser(voucher, user);
        if (userUsage >= voucher.getPerUserLimit()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Voucher per-user limit exceeded");
        }
        if (voucher.getUsageLimit() != null && voucher.getUsedCount() >= voucher.getUsageLimit()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Voucher usage limit exceeded");
        }
        return voucher;
    }

    @Override
    public DiscountPreview calculateDiscount(Voucher voucher, long subtotal) {
        return switch (voucher.getType()) {
            case VoucherTypes.FIXED -> new DiscountPreview(Math.min(voucher.getValue(), subtotal), false);
            case VoucherTypes.PERCENT -> {
                long raw = subtotal * voucher.getValue() / 100L;
                if (voucher.getMaxDiscount() != null) {
                    raw = Math.min(raw, voucher.getMaxDiscount());
                }
                yield new DiscountPreview(Math.min(raw, subtotal), false);
            }
            case VoucherTypes.SHIP -> new DiscountPreview(0L, true);
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported voucher type");
        };
    }

    @Override
    public long calculateShippingFee(long discountedSubtotal, boolean shipVoucher, Voucher voucher) {
        if (discountedSubtotal >= freeShippingThreshold) {
            return 0L;
        }
        if (shipVoucher || (voucher != null && VoucherTypes.SHIP.equals(voucher.getType()))) {
            return 0L;
        }
        return shippingFeeFlat;
    }

    private String buildDescription(Voucher voucher, DiscountPreview preview) {
        return switch (voucher.getType()) {
            case VoucherTypes.FIXED -> "Giảm " + preview.amount() + " VND";
            case VoucherTypes.PERCENT -> "Giảm " + voucher.getValue() + "% (tối đa "
                    + (voucher.getMaxDiscount() != null ? voucher.getMaxDiscount() : "không giới hạn") + " VND)";
            case VoucherTypes.SHIP -> "Miễn phí giao hàng";
            default -> voucher.getType();
        };
    }

    private VoucherResponse mapToResponse(Voucher voucher) {
        return new VoucherResponse(
                voucher.getId(),
                voucher.getCode(),
                voucher.getType(),
                voucher.getValue(),
                voucher.getMinOrder(),
                voucher.getMaxDiscount(),
                voucher.getUsageLimit(),
                voucher.getUsedCount(),
                voucher.getPerUserLimit(),
                voucher.getStartsAt(),
                voucher.getEndsAt(),
                voucher.getActive());
    }
}
