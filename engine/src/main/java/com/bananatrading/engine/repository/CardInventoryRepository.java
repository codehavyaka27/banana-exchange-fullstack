package com.bananatrading.engine.repository;

import com.bananatrading.engine.entity.CardInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CardInventoryRepository extends JpaRepository<CardInventory, Long> {

    // Eagerly fetches Card in the same SQL query (prevents N+1 SELECT queries)
    @Query("SELECT ci FROM CardInventory ci JOIN FETCH ci.card WHERE ci.user.id = :userId")
    List<CardInventory> findByUserIdWithCards(@Param("userId") Long userId);
}