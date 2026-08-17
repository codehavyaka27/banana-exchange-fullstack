package com.bananatrading.engine.service;

import com.bananatrading.engine.entity.Card;
import com.bananatrading.engine.entity.LiquidityPool;
import com.bananatrading.engine.entity.OrderType;
import com.bananatrading.engine.repository.CardRepository;
import com.bananatrading.engine.repository.LiquidityPoolRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

@Service
public class MarketBotService {

    private static final Logger log = LoggerFactory.getLogger(MarketBotService.class);

    private final CardRepository cardRepository;
    private final LiquidityPoolRepository liquidityPoolRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final Random random = new Random();

    public MarketBotService(CardRepository cardRepository, LiquidityPoolRepository liquidityPoolRepository, SimpMessagingTemplate messagingTemplate) {
        this.cardRepository = cardRepository;
        this.liquidityPoolRepository = liquidityPoolRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Scheduled(fixedRate = 1200)
    public void simulateMarketActivity() {
        try {
            List<Card> allCards = cardRepository.findAll();
            if (allCards.isEmpty()) return;

            Card randomCard = allCards.get(random.nextInt(allCards.size()));
            LiquidityPool pool = liquidityPoolRepository.findByCardId(randomCard.getId()).orElse(null);
            if (pool == null) return;

            // --- SINE WAVE MARKET CYCLES ---
            double timeInMinutes = System.currentTimeMillis() / 60000.0;
            double marketCycle = Math.sin(timeInMinutes);

            int sellProbability = (int) (50 - (marketCycle * 15));
            int roll = random.nextInt(100);
            OrderType orderType = (roll < sellProbability) ? OrderType.SELL : OrderType.BUY;
            boolean isWhaleDump = (roll > 90) && (marketCycle <= 0.2);

            // --- PLUNGE PROTECTION ---
            BigDecimal currentPrice = pool.getCashReserve().divide(pool.getCardReserve(), 4, RoundingMode.HALF_UP);
            BigDecimal priceFloor = BigDecimal.ZERO;

            if (randomCard.getName().equalsIgnoreCase("Tiko")) priceFloor = new BigDecimal("2.00");
            else if (randomCard.getName().equalsIgnoreCase("ABM")) priceFloor = new BigDecimal("0.50");
            else if (randomCard.getName().equalsIgnoreCase("Curse")) priceFloor = new BigDecimal("0.05");

            if (currentPrice.compareTo(priceFloor) < 0) {
                orderType = OrderType.BUY;
                isWhaleDump = false;
            }

            // --- BASE TRADE SIZING ---
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

            // --- IN-MEMORY AMM MATH ---
            if (orderType == OrderType.BUY) {
                BigDecimal newCash = pool.getCashReserve().add(amount);
                BigDecimal newCard = pool.getkValue().divide(newCash, 4, RoundingMode.HALF_UP);
                pool.setCashReserve(newCash);
                pool.setCardReserve(newCard);
            } else {
                BigDecimal newCard = pool.getCardReserve().add(amount);
                BigDecimal newCash = pool.getkValue().divide(newCard, 4, RoundingMode.HALF_UP);
                pool.setCardReserve(newCard);
                pool.setCashReserve(newCash);
            }

            // 1. Save updated pool
            liquidityPoolRepository.save(pool);

            // 2. Build live market data payload
            List<Map<String, Object>> liveMarket = new ArrayList<>();
            List<LiquidityPool> allPools = liquidityPoolRepository.findAll();

            for (Card card : allCards) {
                LiquidityPool p = allPools.stream()
                        .filter(lp -> lp.getCard() != null && Objects.equals(lp.getCard().getId(), card.getId()))
                        .findFirst()
                        .orElse(null);

                BigDecimal price = (p != null && p.getCardReserve().compareTo(BigDecimal.ZERO) > 0)
                        ? p.getCashReserve().divide(p.getCardReserve(), 4, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;

                Map<String, Object> item = new HashMap<>();
                item.put("cardId", card.getId());
                item.put("ticker", card.getName());
                item.put("currentPrice", price);
                liveMarket.add(item);
            }

            // 3. Blast JSON payload directly to React clients
            messagingTemplate.convertAndSend("/topic/market", liveMarket);

        } catch (Exception e) {
            log.error("❌ BOT FAIL: {}", e.getMessage());
        }
    }
}