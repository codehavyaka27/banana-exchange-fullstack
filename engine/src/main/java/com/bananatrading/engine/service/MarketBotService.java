package com.bananatrading.engine.service;

import com.bananatrading.engine.entity.Card;
import com.bananatrading.engine.entity.CardInventory;
import com.bananatrading.engine.entity.LiquidityPool;
import com.bananatrading.engine.entity.OrderType;
import com.bananatrading.engine.entity.User;
import com.bananatrading.engine.repository.CardInventoryRepository;
import com.bananatrading.engine.repository.CardRepository;
import com.bananatrading.engine.repository.LiquidityPoolRepository;
import com.bananatrading.engine.repository.UserRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
public class MarketBotService {
    private final OrderService orderService;
    private final UserRepository userRepository;
    private final CardRepository cardRepository;
    private final CardInventoryRepository cardInventoryRepository;
    private final LiquidityPoolRepository liquidityPoolRepository;
    private final Random random = new Random();

    public MarketBotService(OrderService orderService, UserRepository userRepository, CardRepository cardRepository, CardInventoryRepository cardInventoryRepository, LiquidityPoolRepository liquidityPoolRepository){
        this.cardRepository = cardRepository;
        this.orderService = orderService;
        this.userRepository = userRepository;
        this.cardInventoryRepository = cardInventoryRepository;
        this.liquidityPoolRepository = liquidityPoolRepository;
    }

    @Scheduled(fixedRate = 4000)
    public void simulateMarketActivity(){

        User admin = userRepository.findByusername("SystemAdmin").orElse(null);
        if(admin == null) return;

        // --- INFINITE MONEY GLITCH ---
        if (admin.getWalletBalance().compareTo(new BigDecimal("1000000")) < 0) {
            admin.setWalletBalance(new BigDecimal("10000000"));
            userRepository.save(admin);
        }

        List<Card> allCards = cardRepository.findAll();
        if(allCards.isEmpty()) return;

        Card randomCard = allCards.get(random.nextInt(allCards.size()));

        LiquidityPool pool = liquidityPoolRepository.findByCardId(randomCard.getId()).orElse(null);
        if (pool == null) return;

        // ==========================================
        // --- NEW: SINE WAVE MARKET CYCLES ---
        // ==========================================
        // Time in minutes. Divisor controls how fast the market cycle changes (e.g., 60000 = 1 minute cycles)
        double timeInMinutes = System.currentTimeMillis() / 60000.0;
        double marketCycle = Math.sin(timeInMinutes); // Ranges from -1.0 to 1.0

        // Base is 50/50. The sine wave shifts it +/- 15%.
        // Peak Bull = 35% Sell (65% Buy). Peak Bear = 65% Sell (35% Buy).
        int sellProbability = (int) (50 - (marketCycle * 15));

        int roll = random.nextInt(100);
        OrderType orderType = (roll < sellProbability) ? OrderType.SELL : OrderType.BUY;

        // Whale Dumps only happen during Bear cycles or neutral markets
        boolean isWhaleDump = (roll > 90) && (marketCycle <= 0.2);

        // ==========================================
        // --- THE BUY WALL (PLUNGE PROTECTION) ---
        // ==========================================
        BigDecimal currentPrice = pool.getCashReserve().divide(pool.getCardReserve(), 4, RoundingMode.HALF_UP);

        BigDecimal priceFloor = BigDecimal.ZERO;
        if (randomCard.getName().equalsIgnoreCase("Tiko")) priceFloor = new BigDecimal("2.00");
        else if (randomCard.getName().equalsIgnoreCase("ABM")) priceFloor = new BigDecimal("0.50");
        else if (randomCard.getName().equalsIgnoreCase("Curse")) priceFloor = new BigDecimal("0.05");

        boolean isPlungeProtectionActive = false;

        if (currentPrice.compareTo(priceFloor) < 0) {
            orderType = OrderType.BUY;
            isWhaleDump = false;
            isPlungeProtectionActive = true;
        }

        // --- SMART INVENTORY CHECK & REFILL ---
        if (orderType == OrderType.SELL) {
            Optional<CardInventory> botInventoryOpt = cardInventoryRepository.findByUserIdAndCardId(admin.getId(), randomCard.getId());

            CardInventory botInventory = botInventoryOpt.orElseGet(() -> {
                CardInventory newInv = new CardInventory();
                newInv.setUser(admin);
                newInv.setCard(randomCard);
                newInv.setAveragePurchasePrice(BigDecimal.ZERO);
                newInv.setQuantity(new BigDecimal("10000"));
                return cardInventoryRepository.save(newInv);
            });

            if (botInventory.getQuantity().compareTo(new BigDecimal("100")) < 0) {
                botInventory.setQuantity(botInventory.getQuantity().add(new BigDecimal("10000")));
                cardInventoryRepository.save(botInventory);
            }
        }

        // --- BASE TRADE SIZING ---
        int minTrade;
        int maxTrade;

        if (randomCard.getName().equalsIgnoreCase("Tiko")) {
            minTrade = 1000;
            maxTrade = 15000;
        } else if (randomCard.getName().equalsIgnoreCase("ABM")) {
            minTrade = 100;
            maxTrade = 2000;
        } else {
            minTrade = 1;
            maxTrade = 50;
        }

        BigDecimal amount = BigDecimal.valueOf(random.nextInt(maxTrade - minTrade) + minTrade);

        // Whale multipliers
        if (isWhaleDump && orderType == OrderType.SELL) {
            if(randomCard.getName().equalsIgnoreCase("Curse")) {
                amount = amount.multiply(new BigDecimal("1.5"));
            } else {
                amount = amount.multiply(new BigDecimal("2.5"));
            }
        }

        // --- SMART LIQUIDITY CAPPING ---
        BigDecimal maxAllowed;
        if (orderType == OrderType.BUY) {
            maxAllowed = pool.getCashReserve().multiply(new BigDecimal("0.045"));
        } else {
            maxAllowed = pool.getCardReserve().multiply(new BigDecimal("0.045"));
        }

        if (amount.compareTo(maxAllowed) > 0) {
            amount = maxAllowed.setScale(0, RoundingMode.HALF_UP);
        }

        if (amount.compareTo(BigDecimal.ONE) < 0) {
            amount = BigDecimal.ONE;
        }

        // --- EXECUTE TRADE ---
        try {
            orderService.placeOrder(admin.getId(), randomCard.getId(), orderType, amount);

            if (isPlungeProtectionActive) {
                System.out.println("🛡️ PLUNGE PROTECTION ACTIVATED! Bought $" + amount + " of " + randomCard.getName() + " to defend price.");
            } else if (isWhaleDump && orderType == OrderType.SELL) {
                System.out.println("⚠️ WHALE DUMP ON " + randomCard.getName() + "! ($" + amount + ")");
            } else {
                // Determine current market mood for the log
                String mood = (marketCycle > 0) ? "🐂 Bull" : "🐻 Bear";
                System.out.println("✅ BOT (" + mood + ") : " + orderType + " " + amount + " of " + randomCard.getName());
            }
        } catch (IllegalArgumentException e) {
            System.err.println("❌ BOT FAIL (" + randomCard.getName() + "): " + e.getMessage());
        }
    }
}