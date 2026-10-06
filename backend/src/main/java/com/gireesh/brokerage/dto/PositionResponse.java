package com.gireesh.brokerage.dto;

import com.gireesh.brokerage.entity.Position;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PositionResponse(
        Long id,
        Long securityId,
        String symbol,
        String companyName,
        Integer quantity,
        BigDecimal averageCost,
        BigDecimal realizedPnl,
        LocalDateTime updatedAt) {

    public static PositionResponse from(Position position) {
        return new PositionResponse(
                position.getId(),
                position.getSecurity().getId(),
                position.getSecurity().getSymbol(),
                position.getSecurity().getCompanyName(),
                position.getQuantity(),
                position.getAverageCost(),
                position.getRealizedPnl(),
                position.getUpdatedAt());
    }
}
