package com.bananatrading.engine.service;

import com.bananatrading.engine.entity.Card;
import com.bananatrading.engine.entity.LiquidityPool;
import com.bananatrading.engine.entity.OrderType;
import com.bananatrading.engine.repository.CardRepository;
import com.bananatrading.engine.repository.LiquidityPoolRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Random;

@Service
public class MarketBotService {

    private final CardRepository cardRepository;
    private final LiquidityPoolRepository liquidityPoolRepository;
    private final SimpMessagingTemplate messagingTemplate; // <-- We inject the WebSocket blater directly!
    private final Random random = new Random();

    // Ripped out OrderService, UserRepository, and InventoryRepository. The bot is now a Ghost.
    public MarketBotService(CardRepository cardRepository, LiquidityPoolRepository liquidityPoolRepository, SimpMessagingTemplate messagingTemplate){
        this.cardRepository = cardRepository;
        this.liquidityPoolRepository = liquidityPoolRepository;
        this.messagingTemplate = messagingTemplate;
    }

    // 🔥 HIGH FREQUENCY: Runs every 1.2 seconds now because it skips the heavy DB logic
    @Scheduled(fixedRate = 1200)
    public void simulateMarketActivity(){

        List<Card> allCards = cardRepository.findAll();
        if(allCards.isEmpty()) return;

        Card randomCard = allCards.get(random.nextInt(allCards.size()));

        LiquidityPool pool = liquidityPoolRepository.findByCardId(randomCard.getId()).orElse(null);
        if (pool == null) return;

        // ==========================================
        // --- SINE WAVE MARKET CYCLES ---
        // ==========================================
        double timeInMinutes = System.currentTimeMillis() / 60000.0;
        double marketCycle = Math.sin(timeInMinutes);

        int sellProbability = (int) (50 - (marketCycle * 15));
        int roll = random.nextInt(100);
        OrderType orderType = (roll < sellProbability) ? OrderType.SELL : OrderType.BUY;
        boolean isWhaleDump = (roll > 90) && (marketCycle <= 0.2);

        // ==========================================
        // --- PLUNGE PROTECTION ---
        // ==========================================
        BigDecimal currentPrice = pool.getCashReserve().divide(pool.getCardReserve(), 4, RoundingMode.HALF_UP);
        BigDecimal priceFloor = BigDecimal.ZERO;

        if (randomCard.getName().equalsIgnoreCase("Tiko")) priceFloor = new BigDecimal("2.00");
        else if (randomCard.getName().equalsIgnoreCase("ABM")) priceFloor = new BigDecimal("0.50");
        else if (randomCard.getName().equalsIgnoreCase("Curse")) priceFloor = new BigDecimal("0.05");

        if (currentPrice.compareTo(priceFloor) < 0) {
            orderType = OrderType.BUY;
            isWhaleDump = false;
        }

        // ==========================================
        // --- BASE TRADE SIZING ---
        // ==========================================
        int minTrade; int maxTrade;
        if (randomCard.getName().equalsIgnoreCase("Tiko")) { minTrade = 1000; maxTrade = 15000; }
        else if (randomCard.getName().equalsIgnoreCase("ABM")) { minTrade = 100; maxTrade = 2000; }
        else { minTrade = 1; maxTrade = 50; }

        BigDecimal amount = BigDecimal.valueOf(random.nextInt(maxTrade - minTrade) + minTrade);

        if (isWhaleDump && orderType == OrderType.SELL) {
            amount = amount.multiply(randomCard.getName().equalsIgnoreCase("Curse") ? new BigDecimal("1.5") : new BigDecimal("2.5"));
        }

        BigDecimal maxAllowed = (orderType == OrderType.BUY)
                ? pool.getCashReserve().multiply(new BigDecimal("0.045"))
                : pool.getCardReserve().multiply(new BigDecimal("0.045"));

        if (amount.compareTo(maxAllowed) > 0) amount = maxAllowed.setScale(0, RoundingMode.HALF_UP);
        if (amount.compareTo(BigDecimal.ONE) < 0) amount = BigDecimal.ONE;

        // ==========================================
        // --- IN-MEMORY AMM MATH (ZERO DB OVERHEAD) ---
        // ==========================================
        try {
            if (orderType == OrderType.BUY) {
                // Bot spends Cash, removes Card from pool
                BigDecimal newCash = pool.getCashReserve().add(amount);
                BigDecimal newCard = pool.getkValue().divide(newCash, 4, RoundingMode.HALF_UP);
                pool.setCashReserve(newCash);
                pool.setCardReserve(newCard);
            } else {
                // Bot dumps Card, removes Cash from pool
                BigDecimal newCard = pool.getCardReserve().add(amount);
                BigDecimal newCash = pool.getkValue().divide(newCard, 4, RoundingMode.HALF_UP);
                pool.setCardReserve(newCard);
                pool.setCashReserve(newCash);
            }

            // 1. Save only the pool state (Super fast)
            liquidityPoolRepository.save(pool);

            // 2. Blast the WebSocket to React instantly
            messagingTemplate.convertAndSend("/topic/market", "UPDATE");

            String mood = (marketCycle > 0) ? "🐂 Bull" : "🐻 Bear";
            System.out.println("⚡ GHOST BOT (" + mood + ") : " + orderType + " " + amount + " of " + randomCard.getName());

        } catch (Exception e) {
            System.err.println("❌ BOT FAIL: " + e.getMessage());
        }
    }
}