package com.gireesh.brokerage.dto;

import com.gireesh.brokerage.entity.OrderSide;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateOrderRequest(
        @NotNull Long securityId,
        @NotNull OrderSide side,
        @NotNull @Positive Integer quantity) {
}
