package com.bananatrading.engine.controller;

import com.bananatrading.engine.service.CardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.bananatrading.engine.entity.Card;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/cards")
public class CardController {

    private final CardService cardService;
    public CardController(CardService cardService){
        this.cardService=cardService;
    }

    public record MintRequest(Long userId, String cardName, Integer totalSupply, BigDecimal initialCashSeed){}

    @PostMapping("/mint")
    public ResponseEntity<?> mintCard(@RequestBody MintRequest request){
        try{
            Card newCard=cardService.mintNewCard(
                    request.userId(),
                    request.cardName(),
                    request.totalSupply(),
                    request.initialCashSeed()
            );
            return ResponseEntity.ok("Success! you just minted: "+newCard.getName()+". The AMM robot is online and ready for trading!");
        }catch (IllegalArgumentException e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/market")
    public ResponseEntity<?> getMarketTicker(){
        return ResponseEntity.ok(cardService.getLiveMarketPrices());
    }

}
