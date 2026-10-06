package com.gireesh.brokerage.service;

import com.gireesh.brokerage.dto.CreateOrderRequest;
import com.gireesh.brokerage.dto.OrderResponse;
import com.gireesh.brokerage.dto.PageResponse;
import com.gireesh.brokerage.dto.PositionResponse;
import com.gireesh.brokerage.dto.TransactionResponse;
import com.gireesh.brokerage.entity.Order;
import com.gireesh.brokerage.entity.OrderStatus;
import com.gireesh.brokerage.entity.OrderType;
import com.gireesh.brokerage.entity.Position;
import com.gireesh.brokerage.entity.Security;
import com.gireesh.brokerage.entity.TradingAccount;
import com.gireesh.brokerage.entity.Transaction;
import com.gireesh.brokerage.entity.TransactionType;
import com.gireesh.brokerage.exception.ApiException;
import com.gireesh.brokerage.exception.ApiValidationException;
import com.gireesh.brokerage.repository.OrderRepository;
import com.gireesh.brokerage.repository.PositionRepository;
import com.gireesh.brokerage.repository.SecurityRepository;
import com.gireesh.brokerage.repository.TradingAccountRepository;
import com.gireesh.brokerage.repository.TransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;

@Service
public class TradingService {
    private static final int MONEY_SCALE = 2;
    private static final int PRICE_SCALE = 2;
    private static final int AVERAGE_COST_SCALE = 6;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
    private static final BigDecimal ZERO_MONEY = new BigDecimal("0.00");

    private final CurrentUserService currentUserService;
    private final TradingAccountRepository tradingAccountRepository;
    private final SecurityRepository securityRepository;
    private final OrderRepository orderRepository;
    private final PositionRepository positionRepository;
    private final TransactionRepository transactionRepository;

    public TradingService(
            CurrentUserService currentUserService,
            TradingAccountRepository tradingAccountRepository,
            SecurityRepository securityRepository,
            OrderRepository orderRepository,
            PositionRepository positionRepository,
            TransactionRepository transactionRepository) {
        this.currentUserService = currentUserService;
        this.tradingAccountRepository = tradingAccountRepository;
        this.securityRepository = securityRepository;
        this.orderRepository = orderRepository;
        this.positionRepository = positionRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public Submission submit(CreateOrderRequest request, String idempotencyKey) {
        validateIdempotencyKey(idempotencyKey);
        Long authenticatedUserId = currentUserService.currentUserId();
        tradingAccountRepository.acquireWriteLockForUser(authenticatedUserId);
        TradingAccount account = tradingAccountRepository.findByUserIdForUpdate(authenticatedUserId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", "Trading account not found"));
        String fingerprint = fingerprint(request);

        Optional<Order> prior = orderRepository.findByAccountIdAndIdempotencyKey(
                account.getId(), idempotencyKey);
        if (prior.isPresent()) {
            Order existing = prior.get();
            if (!MessageDigest.isEqual(
                    existing.getRequestFingerprint().getBytes(StandardCharsets.US_ASCII),
                    fingerprint.getBytes(StandardCharsets.US_ASCII))) {
                throw new ApiException(
                        HttpStatus.CONFLICT,
                        "IDEMPOTENCY_KEY_REUSED",
                        "Idempotency key was already used for a different order");
            }
            return new Submission(
                    OrderResponse.from(existing), existing.getStatus() == OrderStatus.REJECTED);
        }

        Security security = securityRepository.findById(request.securityId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "SECURITY_NOT_FOUND", "Security not found"));
        BigDecimal executionPrice = security.getCurrentPrice() == null
                ? null
                : security.getCurrentPrice().setScale(PRICE_SCALE, ROUNDING);
        String rejectionCode = getRejectionCode(account, security, request, executionPrice);
        if (rejectionCode != null) {
            Order rejected = newOrder(account, security, request, idempotencyKey, fingerprint);
            rejected.setStatus(OrderStatus.REJECTED);
            rejected.setRejectionReason(rejectionCode);
            Order saved = orderRepository.saveAndFlush(rejected);
            return new Submission(OrderResponse.from(saved), true);
        }

        BigDecimal tradeValue = executionPrice
                .multiply(BigDecimal.valueOf(request.quantity()))
                .setScale(MONEY_SCALE, ROUNDING);
        Position position = positionRepository.findByAccountAndSecurity(account, security).orElse(null);
        BigDecimal realizedPnl = ZERO_MONEY;

        Order order = newOrder(account, security, request, idempotencyKey, fingerprint);
        order.setStatus(OrderStatus.EXECUTED);
        order.setExecutedPrice(executionPrice);
        order.setExecutedAt(LocalDateTime.now());

        if (request.side() == com.gireesh.brokerage.entity.OrderSide.BUY) {
            executeBuy(account, security, request.quantity(), executionPrice, tradeValue, position);
        } else {
            realizedPnl = executeSell(account, position, request.quantity(), executionPrice, tradeValue);
        }

        Order savedOrder = orderRepository.saveAndFlush(order);
        Transaction transaction = new Transaction();
        transaction.setTradingAccount(account);
        transaction.setOrder(savedOrder);
        transaction.setType(TransactionType.valueOf(request.side().name()));
        transaction.setQuantity(request.quantity());
        transaction.setPrice(executionPrice);
        transaction.setTradeValue(tradeValue);
        transaction.setBrokerage(ZERO_MONEY);
        transaction.setTotalAmount(tradeValue);
        transaction.setRealizedPnl(realizedPnl);
        transaction.setTransactionTime(LocalDateTime.now());
        transactionRepository.saveAndFlush(transaction);
        Order persistedOrder = orderRepository.findById(savedOrder.getId()).orElseThrow();
        return new Submission(OrderResponse.from(persistedOrder), false);
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> listOrders(int page, int size) {
        Page<Order> result = orderRepository.findByAccountOrderByCreatedAtDesc(
                currentUserService.currentTradingAccount(), pageRequest(page, size));
        return pageResponse(result, result.map(OrderResponse::from));
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long orderId) {
        return orderRepository.findByIdAndAccount(
                        orderId, currentUserService.currentTradingAccount())
                .map(OrderResponse::from)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "Order not found"));
    }

    @Transactional(readOnly = true)
    public PageResponse<PositionResponse> listPositions(int page, int size) {
        Page<Position> result = positionRepository.findByAccountOrderBySecuritySymbolAsc(
                currentUserService.currentTradingAccount(), pageRequest(page, size));
        return pageResponse(result, result.map(PositionResponse::from));
    }

    @Transactional(readOnly = true)
    public PageResponse<TransactionResponse> listTransactions(int page, int size) {
        Page<Transaction> result = transactionRepository.findByTradingAccountOrderByTransactionTimeDesc(
                currentUserService.currentTradingAccount(), pageRequest(page, size));
        return pageResponse(result, result.map(TransactionResponse::from));
    }

    private void executeBuy(
            TradingAccount account,
            Security security,
            int quantity,
            BigDecimal executionPrice,
            BigDecimal tradeValue,
            Position position) {
        int oldQuantity = position == null ? 0 : position.getQuantity();
        BigDecimal oldAverageCost = position == null ? BigDecimal.ZERO : position.getAverageCost();
        int newQuantity = Math.addExact(oldQuantity, quantity);
        BigDecimal oldCostBasis = oldAverageCost.multiply(BigDecimal.valueOf(oldQuantity))
                .setScale(MONEY_SCALE, ROUNDING);
        BigDecimal oldCost = oldAverageCost.multiply(BigDecimal.valueOf(oldQuantity));
        BigDecimal newAverageCost = oldCost.add(tradeValue)
                .divide(BigDecimal.valueOf(newQuantity), AVERAGE_COST_SCALE, ROUNDING);
        BigDecimal newCostBasis = newAverageCost.multiply(BigDecimal.valueOf(newQuantity))
                .setScale(MONEY_SCALE, ROUNDING);

        if (position == null) {
            position = new Position();
            position.setAccount(account);
            position.setSecurity(security);
            position.setRealizedPnl(ZERO_MONEY);
        }
        position.setQuantity(newQuantity);
        position.setAverageCost(newAverageCost);
        positionRepository.save(position);

        account.setCash(account.getCash().subtract(tradeValue).setScale(MONEY_SCALE, ROUNDING));
        account.setInvestedCapital(account.getInvestedCapital()
                .add(newCostBasis.subtract(oldCostBasis)).setScale(MONEY_SCALE, ROUNDING));
        tradingAccountRepository.save(account);
    }

    private BigDecimal executeSell(
            TradingAccount account,
            Position position,
            int quantity,
            BigDecimal executionPrice,
            BigDecimal tradeValue) {
        BigDecimal realizedPnl = executionPrice
                .subtract(position.getAverageCost())
                .multiply(BigDecimal.valueOf(quantity))
                .setScale(MONEY_SCALE, ROUNDING);
        BigDecimal releasedCost = position.getAverageCost()
                .multiply(BigDecimal.valueOf(quantity))
                .setScale(MONEY_SCALE, ROUNDING);

        account.setCash(account.getCash().add(tradeValue).setScale(MONEY_SCALE, ROUNDING));
        account.setInvestedCapital(account.getInvestedCapital()
                .subtract(releasedCost).setScale(MONEY_SCALE, ROUNDING));
        account.setRealizedPnl(account.getRealizedPnl()
                .add(realizedPnl).setScale(MONEY_SCALE, ROUNDING));
        tradingAccountRepository.save(account);

        position.setRealizedPnl(position.getRealizedPnl()
                .add(realizedPnl).setScale(MONEY_SCALE, ROUNDING));
        int remaining = position.getQuantity() - quantity;
        if (remaining == 0) {
            positionRepository.delete(position);
        } else {
            position.setQuantity(remaining);
            positionRepository.save(position);
        }
        return realizedPnl;
    }

    private String getRejectionCode(
            TradingAccount account, Security security, CreateOrderRequest request, BigDecimal executionPrice) {
        if (executionPrice == null || executionPrice.signum() <= 0) {
            return "INVALID_MARKET_PRICE";
        }
        BigDecimal tradeValue = executionPrice
                .multiply(BigDecimal.valueOf(request.quantity()))
                .setScale(MONEY_SCALE, ROUNDING);
        if (request.side() == com.gireesh.brokerage.entity.OrderSide.BUY) {
            return account.getCash().compareTo(tradeValue) < 0 ? "INSUFFICIENT_CASH" : null;
        }
        Optional<Position> position = positionRepository.findByAccountAndSecurity(account, security);
        return position.isEmpty() || position.get().getQuantity() < request.quantity()
                ? "INSUFFICIENT_HOLDINGS"
                : null;
    }

    private Order newOrder(
            TradingAccount account,
            Security security,
            CreateOrderRequest request,
            String idempotencyKey,
            String fingerprint) {
        Order order = new Order();
        order.setAccount(account);
        order.setSecurity(security);
        order.setSide(request.side());
        order.setType(OrderType.MARKET);
        order.setQuantity(request.quantity());
        order.setCharges(ZERO_MONEY);
        order.setIdempotencyKey(idempotencyKey);
        order.setRequestFingerprint(fingerprint);
        return order;
    }

    private void validateIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > 100) {
            throw new ApiValidationException(
                    "VALIDATION_FAILED",
                    "Request validation failed",
                    Map.of("Idempotency-Key", "Idempotency-Key must contain 1 to 100 non-blank characters"));
        }
    }

    private String fingerprint(CreateOrderRequest request) {
        String canonical = "MARKET|" + request.side().name() + "|"
                + request.securityId() + "|" + request.quantity();
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private PageRequest pageRequest(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new ApiValidationException(
                    "VALIDATION_FAILED",
                    "Request validation failed",
                    Map.of("page", "Page must be non-negative and size must be between 1 and 100"));
        }
        return PageRequest.of(page, size);
    }

    private <T, R> PageResponse<R> pageResponse(Page<T> source, Page<R> mapped) {
        return new PageResponse<>(
                mapped.getContent(),
                mapped.getNumber(),
                mapped.getSize(),
                source.getTotalElements(),
                source.getTotalPages());
    }

    public record Submission(OrderResponse order, boolean rejected) {
    }
}
