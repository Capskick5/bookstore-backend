package edu.fpt.sba301.bookstore.mapper;

import edu.fpt.sba301.bookstore.dto.response.AdminOrderResponse;
import edu.fpt.sba301.bookstore.dto.response.OrderItemResponse;
import edu.fpt.sba301.bookstore.dto.response.OrderResponse;
import edu.fpt.sba301.bookstore.entity.Order;
import edu.fpt.sba301.bookstore.entity.OrderItem;
import edu.fpt.sba301.bookstore.entity.User;
import edu.fpt.sba301.bookstore.repository.OrderItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class OrderMapper {

    private final OrderItemRepository orderItemRepository;

    public OrderResponse toResponse(Order order, String paymentUrl) {
        List<OrderItemResponse> itemResponses = mapItems(order.getId());
        return new OrderResponse(
                order.getId(),
                order.getStatus(),
                order.getPaymentMethod(),
                order.getSubtotal(),
                order.getDiscount(),
                order.getShippingFee(),
                order.getTotal(),
                order.getVoucherCode(),
                order.getPointsUsed(),
                order.getPointsEarned(),
                paymentUrl,
                order.getAddressSnapshot(),
                order.getExpiresAt(),
                order.getCreatedAt(),
                order.getManualRefundRequired(),
                itemResponses
        );
    }

    public AdminOrderResponse toAdminResponse(Order order) {
        User user = order.getUser();
        return new AdminOrderResponse(
                order.getId(),
                order.getStatus(),
                order.getPaymentMethod(),
                order.getSubtotal(),
                order.getDiscount(),
                order.getShippingFee(),
                order.getTotal(),
                order.getVoucherCode(),
                order.getPointsUsed(),
                order.getPointsEarned(),
                user.getEmail(),
                user.getFullName(),
                order.getAddressSnapshot(),
                order.getExpiresAt(),
                order.getCreatedAt(),
                order.getManualRefundRequired(),
                mapItems(order.getId())
        );
    }

    private List<OrderItemResponse> mapItems(Long orderId) {
        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        return items.stream()
                .map(item -> new OrderItemResponse(
                        item.getBook().getId(),
                        item.getTitleSnapshot(),
                        item.getUnitPrice(),
                        item.getQuantity()))
                .toList();
    }
}
