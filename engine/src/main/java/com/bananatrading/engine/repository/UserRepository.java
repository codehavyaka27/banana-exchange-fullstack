package com.bananatrading.engine.repository;

import com.bananatrading.engine.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    // Single-trip query: grabs user and their inventory positions together
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.inventory WHERE u.id = :id")
    Optional<User> findByIdWithInventory(@Param("id") Long id);
}