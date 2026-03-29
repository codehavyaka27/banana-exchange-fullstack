package com.bananatrading.engine.dto;

import java.math.BigDecimal;
import java.util.List;

public record UserProfileDTO(String username,BigDecimal walletBalance,List<PortfolioItemDTO> inventory) {

}
