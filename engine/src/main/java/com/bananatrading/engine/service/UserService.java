package com.bananatrading.engine.service;

import com.bananatrading.engine.dto.PortfolioItemDTO;
import com.bananatrading.engine.dto.UserProfileDTO;

import com.bananatrading.engine.entity.CardInventory;
import com.bananatrading.engine.repository.CardInventoryRepository;

import com.bananatrading.engine.repository.UserRepository;
import com.bananatrading.engine.entity.User;
import com.bananatrading.engine.security.JwtUtil;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final CardInventoryRepository cardInventoryRepository;

    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    private static final BigDecimal INITIAL_WALLET_BALANCE = new BigDecimal("10000.00");

    public UserService(UserRepository userRepository,  CardInventoryRepository cardInventoryRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil){
        this.userRepository = userRepository;

        this.cardInventoryRepository = cardInventoryRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }
    @Transactional
    public String registerNewPlayer(String username,String rawPassword){

        Optional<User> existingUser = userRepository.findByUsername(username);
        if(existingUser.isPresent()){
            throw new IllegalArgumentException("username is already taken");
        }

        //creating new user
        User newUser = new User();
        newUser.setUsername(username);
        newUser.setPassword(passwordEncoder.encode(rawPassword));
        newUser.setWalletBalance(INITIAL_WALLET_BALANCE);

        userRepository.save(newUser);

        return newUser.getUsername();
    }

    public String loginPlayer(String username, String rawPassword) {
        // 1. Find the user
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));

        // 2. Check if the password matches the scrambled hash in the database
        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new BadCredentialsException("Invalid username or password");
        }

        // 3. Password is correct! Print and return the VIP wristband.
        return jwtUtil.generateToken(user.getUsername(), user.getId());
    }
    @Transactional(readOnly = true)
    public UserProfileDTO getUserPortfolio(Long userId) {
        // 1. Get the User and their wallet balance
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // 2. Get all the items in their backpack
        List<CardInventory> inventory = cardInventoryRepository.findByUserIdWithCards(userId);

        // 3. Loop through the backpack and translate Card IDs into Ticker Names AND grab the Avg Price
        List<PortfolioItemDTO> portfolioItems = inventory.stream()
                .map(inv -> {
                    // Safety check just in case older database rows have a null average price
                    BigDecimal avgPrice = inv.getAveragePurchasePrice() != null
                            ? inv.getAveragePurchasePrice()
                            : BigDecimal.ZERO;

                    return new PortfolioItemDTO(
                            inv.getCard().getName(),
                            inv.getQuantity(),
                            avgPrice // <-- Here is the missing 3rd argument!
                    );
                })
                .toList();

        // 4. Package it all up into our clean DTO
        return new UserProfileDTO(user.getUsername(), user.getWalletBalance(), portfolioItems);
    }
}