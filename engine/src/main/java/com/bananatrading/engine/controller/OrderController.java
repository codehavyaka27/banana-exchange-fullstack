package com.bananatrading.engine.controller;

import com.bananatrading.engine.entity.StockOrder;
import com.bananatrading.engine.entity.OrderType;
import com.bananatrading.engine.service.OrderService;
import com.bananatrading.engine.repository.OrderRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final OrderRepository orderRepository;

    // Inject BOTH the Service (for trading) and Repository (for history)
    public OrderController(OrderService orderService, OrderRepository orderRepository){
        this.orderService = orderService;
        this.orderRepository = orderRepository;
    }

    public record TradeRequest(Long userId, Long cardId, OrderType orderType , BigDecimal tradeAmount){}

    // --- ENDPOINT 1: THE EXECUTION ENGINE ---
    @PostMapping("/trade")
    public ResponseEntity<?> placeTrade(@RequestBody TradeRequest request){
        try{
            StockOrder stockOrder = orderService.placeOrder(
                    request.userId(),
                    request.cardId(),
                    request.orderType(),
                    request.tradeAmount()
            );
            return ResponseEntity.ok(stockOrder);
        }catch (IllegalArgumentException e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // --- ENDPOINT 2: THE TERMINAL LEDGER ---
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<StockOrder>> getUserOrderHistory(@PathVariable Long userId) {
        // Fetch the ledger, filter for this specific user, and sort newest-first
        List<StockOrder> userOrders = orderRepository.findAll().stream()
                .filter(order -> order.getUser().getId().equals(userId))
                .sorted((o1, o2) -> o2.getId().compareTo(o1.getId()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(userOrders);
    }
}