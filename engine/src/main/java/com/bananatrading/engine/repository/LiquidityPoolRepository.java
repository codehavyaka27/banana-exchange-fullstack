package com.bananatrading.engine.repository;

import com.bananatrading.engine.entity.LiquidityPool;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

public interface LiquidityPoolRepository extends JpaRepository<LiquidityPool,Long> {
    Optional<LiquidityPool> findByCardId(Long cardId);

}
