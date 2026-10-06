package com.gireesh.brokerage.dto;

import com.gireesh.brokerage.entity.Order;
import com.gireesh.brokerage.entity.OrderSide;
import com.gireesh.brokerage.entity.OrderStatus;
import com.gireesh.brokerage.entity.OrderType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public record OrderResponse(
        Long id,
        Long securityId,
        String symbol,
        OrderSide side,
        OrderType type,
        Integer quantity,
        BigDecimal executedPrice,
        BigDecimal charges,
        OrderStatus status,
        String rejectionReason,
        LocalDateTime createdAt,
        LocalDateTime executedAt) {

    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getSecurity().getId(),
                order.getSecurity().getSymbol(),
                order.getSide(),
                order.getType(),
                order.getQuantity(),
                order.getExecutedPrice() == null
                        ? null
                        : order.getExecutedPrice().setScale(2, java.math.RoundingMode.HALF_UP),
                order.getCharges(),
                order.getStatus(),
                order.getRejectionReason(),
                roundToDatabasePrecision(order.getCreatedAt()),
                roundToDatabasePrecision(order.getExecutedAt()));
    }

    private static LocalDateTime roundToDatabasePrecision(LocalDateTime timestamp) {
        return timestamp == null
                ? null
                : timestamp.plusNanos(500).truncatedTo(ChronoUnit.MICROS);
    }
}
