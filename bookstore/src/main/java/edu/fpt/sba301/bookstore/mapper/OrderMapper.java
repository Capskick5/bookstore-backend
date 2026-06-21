package edu.fpt.sba301.bookstore.mapper;

import edu.fpt.sba301.bookstore.dto.response.OrderItemResponse;
import edu.fpt.sba301.bookstore.dto.response.OrderResponse;
import edu.fpt.sba301.bookstore.entity.Order;
import edu.fpt.sba301.bookstore.entity.OrderItem;
import edu.fpt.sba301.bookstore.repository.OrderItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class OrderMapper {

    private final OrderItemRepository orderItemRepository;

    public OrderResponse toResponse(Order order, String paymentUrl) {
        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
        List<OrderItemResponse> itemResponses = items.stream()
                .map(item -> new OrderItemResponse(
                        item.getBook().getId(),
                        item.getTitleSnapshot(),
                        item.getUnitPrice(),
                        item.getQuantity()))
                .toList();
        return new OrderResponse(
                order.getId(),
                order.getStatus(),
                order.getSubtotal(),
                order.getDiscount(),
                order.getShippingFee(),
                order.getTotal(),
                order.getVoucherCode(),
                order.getPointsUsed(),
                order.getPointsEarned(),
                paymentUrl,
                order.getExpiresAt(),
                order.getCreatedAt(),
                order.getManualRefundRequired(),
                itemResponses
        );
    }
}
