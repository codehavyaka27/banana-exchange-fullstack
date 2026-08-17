package com.bananatrading.engine.controller;

import com.bananatrading.engine.entity.StockOrder;
import com.bananatrading.engine.entity.OrderType;
import com.bananatrading.engine.service.OrderService;
import com.bananatrading.engine.repository.OrderRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final OrderRepository orderRepository;

    public OrderController(OrderService orderService, OrderRepository orderRepository) {
        this.orderService = orderService;
        this.orderRepository = orderRepository;
    }

    public record TradeRequest(Long userId, Long cardId, OrderType orderType, BigDecimal tradeAmount) {}

    // --- ENDPOINT 1: THE EXECUTION ENGINE ---
    @PostMapping("/trade")
    public ResponseEntity<?> placeTrade(@RequestBody TradeRequest request) {
        try {
            StockOrder stockOrder = orderService.placeOrder(
                    request.userId(),
                    request.cardId(),
                    request.orderType(),
                    request.tradeAmount()
            );
            return ResponseEntity.ok(stockOrder);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // --- ENDPOINT 2: THE TERMINAL LEDGER (INDEX-OPTIMIZED) ---
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<StockOrder>> getUserOrderHistory(@PathVariable Long userId) {
        // Leverages PostgreSQL index: idx_orders_user_time
        List<StockOrder> userOrders = orderRepository.findByUserIdOrderByTimestampDesc(userId);
        return ResponseEntity.ok(userOrders);
    }
}