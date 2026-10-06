package com.gireesh.brokerage.controller;

import com.gireesh.brokerage.dto.CreateOrderRequest;
import com.gireesh.brokerage.dto.OrderResponse;
import com.gireesh.brokerage.dto.PageResponse;
import com.gireesh.brokerage.dto.PositionResponse;
import com.gireesh.brokerage.dto.TransactionResponse;
import com.gireesh.brokerage.entity.OrderStatus;
import com.gireesh.brokerage.exception.ApiException;
import com.gireesh.brokerage.service.TradingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class TradingController {
    private final TradingService tradingService;

    public TradingController(TradingService tradingService) {
        this.tradingService = tradingService;
    }

    @PostMapping("/api/orders")
    public ResponseEntity<OrderResponse> submitOrder(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreateOrderRequest request) {
        TradingService.Submission submission = tradingService.submit(request, idempotencyKey);
        if (submission.rejected()) {
            throw rejection(submission.order());
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(submission.order());
    }

    @GetMapping("/api/orders")
    public PageResponse<OrderResponse> listOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return tradingService.listOrders(page, size);
    }

    @GetMapping("/api/orders/{orderId}")
    public OrderResponse getOrder(@PathVariable Long orderId) {
        return tradingService.getOrder(orderId);
    }

    @GetMapping("/api/positions")
    public PageResponse<PositionResponse> listPositions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return tradingService.listPositions(page, size);
    }

    @GetMapping("/api/transactions")
    public PageResponse<TransactionResponse> listTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return tradingService.listTransactions(page, size);
    }

    private ApiException rejection(OrderResponse order) {
        return switch (order.rejectionReason()) {
            case "INSUFFICIENT_CASH" -> new ApiException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "INSUFFICIENT_CASH",
                    "Insufficient cash to execute this buy order");
            case "INSUFFICIENT_HOLDINGS" -> new ApiException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "INSUFFICIENT_HOLDINGS",
                    "Insufficient holdings to execute this sell order");
            default -> new ApiException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "INVALID_MARKET_PRICE",
                    "The security does not have a valid market price");
        };
    }
}
