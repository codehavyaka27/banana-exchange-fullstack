package com.bananatrading.engine.repository;

import com.bananatrading.engine.entity.CardInventory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CardInventoryRepository extends JpaRepository<CardInventory, Long> {
    Optional<CardInventory> findByUserIdAndCardId(Long userId,Long CardId);
    List<CardInventory> findByUserId(Long userId);
}
