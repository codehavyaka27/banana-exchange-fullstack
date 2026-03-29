package com.bananatrading.engine.dto;

import java.math.BigDecimal;
public record MarketTickerDTO (Long cardId,String ticker,BigDecimal currentPrice){
}
