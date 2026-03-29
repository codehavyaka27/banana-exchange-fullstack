package com.bananatrading.engine.repository;

import com.bananatrading.engine.entity.OrderType;
import com.bananatrading.engine.entity.StockOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<StockOrder,Long> {
    List<StockOrder> findByTickerAndOrderType(String ticker,OrderType orderType);
}
