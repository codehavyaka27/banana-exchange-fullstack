package com.bananatrading.engine.service;
import com.bananatrading.engine.dto.MarketTickerDTO;
import com.bananatrading.engine.repository.UserRepository;
import com.bananatrading.engine.repository.CardRepository;
import com.bananatrading.engine.repository.LiquidityPoolRepository;
import com.bananatrading.engine.entity.LiquidityPool;
import com.bananatrading.engine.entity.Card;
import com.bananatrading.engine.entity.User;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class CardService {
    private final UserRepository userRepository;
    private final CardRepository cardRepository;
    private final LiquidityPoolRepository liquidityPoolRepository;

    public CardService(UserRepository userRepository,CardRepository cardRepository,LiquidityPoolRepository liquidityPoolRepository){
        this.cardRepository=cardRepository;
        this.liquidityPoolRepository=liquidityPoolRepository;
        this.userRepository=userRepository;
    }

    public List<MarketTickerDTO> getLiveMarketPrices(){
        return cardRepository.findAll().stream().map(card -> {
            LiquidityPool pool=liquidityPoolRepository.findByCardId(card.getId()).orElseThrow(()->new RuntimeException("pool not found"));
            BigDecimal currentPrice=pool.getCashReserve().divide(pool.getCardReserve(),4, RoundingMode.HALF_UP);
            return new MarketTickerDTO(card.getId(),card.getName(),currentPrice);
        }).toList();
    }

    @Transactional
    public Card mintNewCard(Long userId,String cardName,Integer totalSupply,BigDecimal initialCashSeed){
        //identifying the player
        User creator=userRepository.findById(userId).orElseThrow(()-> new IllegalArgumentException("user not found in db!"));

        //preventing duplicate coins
        if(cardRepository.findByName(cardName).isPresent()){
            throw new IllegalArgumentException("that card name is already taken!");
        }
        BigDecimal mintingFee=new BigDecimal("100000.00");
        BigDecimal totalCost=mintingFee.add(initialCashSeed);

        if(creator.getWalletBalance().compareTo(totalCost)<0){
            throw new IllegalArgumentException("not enough cash!");
        }

        creator.setWalletBalance(creator.getWalletBalance().subtract(totalCost));
        userRepository.save(creator);
        //minting the official card
        Card newCard=new Card();
        newCard.setCreator(creator);
        newCard.setName(cardName);
        newCard.setTotalSupply(totalSupply);
        Card savedCard=cardRepository.save(newCard);
        //building AMM robot
        BigDecimal poolCardAmount=BigDecimal.valueOf(totalSupply).multiply(new BigDecimal("0.80"));

        LiquidityPool pool=new LiquidityPool();
        pool.setCard(savedCard);
        pool.setCashReserve(initialCashSeed);
        pool.setCardReserve(poolCardAmount);

        pool.setkValue(initialCashSeed.multiply(poolCardAmount));

        liquidityPoolRepository.save((pool));
        return savedCard;

    }
}
