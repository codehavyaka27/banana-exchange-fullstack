package com.bananatrading.engine.dto;
import java.math.BigDecimal;

public record PortfolioItemDTO(String ticker,BigDecimal quantity,BigDecimal averagePurchasePrice) {


}
