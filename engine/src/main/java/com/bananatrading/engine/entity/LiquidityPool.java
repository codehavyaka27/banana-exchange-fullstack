package com.bananatrading.engine.entity;
import jakarta.persistence.*;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Entity
@Table(name="liquidity_pools")
public class LiquidityPool {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name="card_id",nullable = false)
    private Card card;

    @Column(nullable = false,precision = 38,scale=2)
    private BigDecimal cashReserve;

    @Column(nullable = false,precision = 38,scale = 2)
    private BigDecimal cardReserve;

    @Column(nullable = false,precision = 38,scale = 2)
    private BigDecimal kValue;

    public LiquidityPool(){}

    public Long getId(){
        return id;
    }

    public void setId(Long id){
        this.id=id;
    }
    public Card getCard(){
        return card;
    }
    public void setCard(Card card){
        this.card=card;
    }
    public BigDecimal getCashReserve(){
        return cashReserve;
    }
    public void setCashReserve(BigDecimal cashReserve){
        this.cashReserve=cashReserve;
    }
    public BigDecimal getCardReserve(){
        return cardReserve;
    }
    public void setCardReserve(BigDecimal cardReserve){
        this.cardReserve=cardReserve;
    }
    public BigDecimal getkValue(){
        return kValue;
    }
    public void setkValue(BigDecimal kValue){
        this.kValue=kValue;
    }



}
