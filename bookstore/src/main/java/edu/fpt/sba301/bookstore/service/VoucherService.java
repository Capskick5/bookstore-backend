package edu.fpt.sba301.bookstore.service;

import edu.fpt.sba301.bookstore.dto.request.ApplyVoucherRequest;
import edu.fpt.sba301.bookstore.dto.response.ApplyVoucherResponse;
import edu.fpt.sba301.bookstore.dto.response.VoucherResponse;
import edu.fpt.sba301.bookstore.entity.User;
import edu.fpt.sba301.bookstore.entity.Voucher;

import java.util.List;

public interface VoucherService {
    ApplyVoucherResponse applyVoucher(User user, ApplyVoucherRequest request);

    List<VoucherResponse> listApplicableVouchers(User user);

    Voucher validateVoucher(String code, User user, long subtotal);

    DiscountPreview calculateDiscount(Voucher voucher, long subtotal);

    long calculateShippingFee(long discountedSubtotal, boolean shipVoucher, Voucher voucher);

    record DiscountPreview(long amount, boolean shipVoucher) {
    }
}
