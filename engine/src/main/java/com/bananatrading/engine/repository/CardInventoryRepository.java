package com.bananatrading.engine.repository;

import com.bananatrading.engine.entity.CardInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CardInventoryRepository extends JpaRepository<CardInventory, Long> {

    // 1. Used by OrderService to locate an existing inventory slot
    Optional<CardInventory> findByUserIdAndCardId(Long userId, Long cardId);

    // 2. Used by UserService for portfolio lookups (prevents N+1 SELECTs)
    @Query("SELECT ci FROM CardInventory ci JOIN FETCH ci.card WHERE ci.user.id = :userId")
    List<CardInventory> findByUserIdWithCards(@Param("userId") Long userId);
}