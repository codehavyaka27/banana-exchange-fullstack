package com.bananatrading.engine.repository;

import com.bananatrading.engine.entity.OrderType;
import com.bananatrading.engine.entity.StockOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<StockOrder, Long> {

    // Fast matching engine lookup
    List<StockOrder> findByTickerAndOrderType(String ticker, OrderType orderType);

    // Fast trade history query for frontend dashboard (indexed by user & time)
    @Query("SELECT o FROM StockOrder o WHERE o.user.id = :userId ORDER BY o.timestamp DESC")
    List<StockOrder> findByUserIdOrderByTimestampDesc(@Param("userId") Long userId);
}