package com.bananatrading.engine.service;

import com.bananatrading.engine.repository.CardInventoryRepository;
import com.bananatrading.engine.entity.CardInventory;
import com.bananatrading.engine.entity.OrderType;
import com.bananatrading.engine.entity.StockOrder;
import com.bananatrading.engine.entity.User;
import com.bananatrading.engine.entity.LiquidityPool;
import com.bananatrading.engine.repository.LiquidityPoolRepository;
import com.bananatrading.engine.repository.OrderRepository;
import com.bananatrading.engine.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.math.BigDecimal;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final LiquidityPoolRepository liquidityPoolRepository;
    private final CardInventoryRepository cardInventoryRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public OrderService(OrderRepository orderRepository, UserRepository userRepository, LiquidityPoolRepository liquidityPoolRepository, CardInventoryRepository cardInventoryRepository, SimpMessagingTemplate messagingTemplate) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.liquidityPoolRepository = liquidityPoolRepository;
        this.cardInventoryRepository = cardInventoryRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public StockOrder placeOrder(Long userId, Long cardId, OrderType orderType, BigDecimal tradeAmount) {

        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("user ID not found in DB!"));
        LiquidityPool liquidityPool = liquidityPoolRepository.findByCardId(cardId).orElseThrow(() -> new IllegalArgumentException("Liquidity pool not found for this card!"));

        BigDecimal cashReserve = liquidityPool.getCashReserve();
        BigDecimal cardReserve = liquidityPool.getCardReserve();
        BigDecimal kValue = liquidityPool.getkValue();

        BigDecimal priceExecuted;
        BigDecimal quantityExecuted;
        BigDecimal realizedPnl = null;
        BigDecimal entryPrice = null; // <-- FIX: Declare this at the top level!

        if (orderType == OrderType.BUY) {
            BigDecimal cashIn = tradeAmount;

            BigDecimal feeRate = new BigDecimal("0.002");
            BigDecimal fee = cashIn.multiply(feeRate);
            BigDecimal effectiveCashIn = cashIn.subtract(fee);

            BigDecimal maxTradeLimit = cashReserve.multiply(new BigDecimal("0.05"));
            if (cashIn.compareTo(maxTradeLimit) > 0) {
                throw new IllegalArgumentException("Anti whale block! the amount is >5% of pool's cash reserve!..Max allowed right now is $" + maxTradeLimit.setScale(2, RoundingMode.HALF_UP));
            }
            if (user.getWalletBalance().compareTo(cashIn) < 0) {
                throw new IllegalArgumentException("not enough cash!");
            }

            BigDecimal newCashReserve = cashReserve.add(effectiveCashIn);
            BigDecimal newCardReserve = kValue.divide(newCashReserve, 4, RoundingMode.HALF_UP);
            BigDecimal cardsOut = cardReserve.subtract(newCardReserve);

            liquidityPool.setCashReserve(newCashReserve);
            liquidityPool.setCardReserve(newCardReserve);

            user.setWalletBalance(user.getWalletBalance().subtract(cashIn));

            CardInventory inventorySlot = cardInventoryRepository.findByUserIdAndCardId(userId, cardId).orElseGet(() -> {
                CardInventory newSlot = new CardInventory();
                newSlot.setCard(liquidityPool.getCard());
                newSlot.setQuantity(BigDecimal.ZERO);
                newSlot.setUser(user);
                newSlot.setAveragePurchasePrice(BigDecimal.ZERO);
                return newSlot;
            });

            BigDecimal oldQty = inventorySlot.getQuantity() != null ? inventorySlot.getQuantity() : BigDecimal.ZERO;
            BigDecimal oldAvgPrice = inventorySlot.getAveragePurchasePrice() != null ? inventorySlot.getAveragePurchasePrice() : BigDecimal.ZERO;

            BigDecimal oldTotalCost = oldQty.multiply(oldAvgPrice);
            BigDecimal newTotalCost = oldTotalCost.add(cashIn);
            BigDecimal newQty = oldQty.add(cardsOut);
            inventorySlot.setQuantity(newQty);

            if (newQty.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal newAvgPrice = newTotalCost.divide(newQty, 4, RoundingMode.HALF_UP);
                inventorySlot.setAveragePurchasePrice(newAvgPrice);
            }

            cardInventoryRepository.save(inventorySlot);

            priceExecuted = cashIn.divide(cardsOut, 4, RoundingMode.HALF_UP);
            quantityExecuted = cardsOut;

        } else {
            BigDecimal cardsIn = tradeAmount;

            BigDecimal feeRate = new BigDecimal("0.002");
            BigDecimal fee = cardsIn.multiply(feeRate);
            BigDecimal effectiveCardsIn = cardsIn.subtract(fee);

            BigDecimal maxSellLimit = cardReserve.multiply(new BigDecimal("0.05"));
            if (cardsIn.compareTo(maxSellLimit) > 0) {
                throw new IllegalArgumentException("Anti whale block! the amount is >5% of pool's card reserve!..Max allowed right now is " + maxSellLimit.setScale(0, RoundingMode.HALF_UP) + " cards");
            }

            CardInventory inventorySlot = cardInventoryRepository.findByUserIdAndCardId(userId, cardId).orElseThrow(() -> new IllegalArgumentException("you dont own any of these cards!"));

            if (inventorySlot.getQuantity().compareTo(cardsIn) < 0) {
                throw new IllegalArgumentException("Insufficient balance! you only own " + inventorySlot.getQuantity() + " cards");
            }

            BigDecimal newCardReserve = cardReserve.add(effectiveCardsIn);
            BigDecimal newCashReserve = kValue.divide(newCardReserve, 4, RoundingMode.HALF_UP);
            BigDecimal cashOut = cashReserve.subtract(newCashReserve);

            liquidityPool.setCashReserve(newCashReserve);
            liquidityPool.setCardReserve(newCardReserve);

            user.setWalletBalance(user.getWalletBalance().add(cashOut));

            BigDecimal avgEntryPrice = inventorySlot.getAveragePurchasePrice();
            entryPrice = avgEntryPrice; // <-- FIX: Capture it safely here!

            BigDecimal originalCost = cardsIn.multiply(avgEntryPrice);
            realizedPnl = cashOut.subtract(originalCost);

            inventorySlot.setQuantity(inventorySlot.getQuantity().subtract(cardsIn));
            cardInventoryRepository.save(inventorySlot);

            priceExecuted = cashOut.divide(cardsIn, 4, RoundingMode.HALF_UP);
            quantityExecuted = cardsIn;
        }

        liquidityPoolRepository.save(liquidityPool);
        userRepository.save(user);

        StockOrder order = new StockOrder();
        order.setUser(user);
        order.setOrderType(orderType);
        order.setPrice(priceExecuted);
        order.setTicker(liquidityPool.getCard().getName());
        order.setQuantity(quantityExecuted.intValue());
        order.setRealizedPnl(realizedPnl);
        order.setEntryPrice(entryPrice); // <-- FIX: Attach it securely here!

        StockOrder savedOrder = orderRepository.save(order);

        messagingTemplate.convertAndSend("/topic/market", "UPDATE");

        return savedOrder;
    }
}