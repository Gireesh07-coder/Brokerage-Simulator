package com.gireesh.brokerage.dto;

import com.gireesh.brokerage.entity.Transaction;
import com.gireesh.brokerage.entity.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponse(
        Long id,
        Long orderId,
        Long securityId,
        String symbol,
        TransactionType type,
        Integer quantity,
        BigDecimal price,
        BigDecimal tradeValue,
        BigDecimal brokerage,
        BigDecimal totalAmount,
        BigDecimal realizedPnl,
        LocalDateTime transactionTime) {

    public static TransactionResponse from(Transaction transaction) {
        var security = transaction.getOrder() == null
                ? null
                : transaction.getOrder().getSecurity();
        var legacyStock = transaction.getStock();
        return new TransactionResponse(
                transaction.getId(),
                transaction.getOrder() == null ? null : transaction.getOrder().getId(),
                security == null ? null : security.getId(),
                security == null
                        ? (legacyStock == null ? null : legacyStock.getSymbol())
                        : security.getSymbol(),
                transaction.getType(),
                transaction.getQuantity(),
                transaction.getPrice(),
                transaction.getTradeValue(),
                transaction.getBrokerage(),
                transaction.getTotalAmount(),
                transaction.getRealizedPnl(),
                transaction.getTransactionTime());
    }
}
