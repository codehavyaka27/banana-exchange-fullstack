package com.bananatrading.engine.controller;

import com.bananatrading.engine.dto.UserProfileDTO;
import com.bananatrading.engine.entity.User;
import com.bananatrading.engine.repository.UserRepository;
import com.bananatrading.engine.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;

    public UserController(UserService userService, UserRepository userRepository){
        this.userService=userService;
        this.userRepository=userRepository;
    }

    // THE FIX: Changed @RequestParam to @RequestBody to accept the JSON payload
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody Map<String, String> credentials){
        try{
            String username = credentials.get("username");
            String password = credentials.get("password");
            User savedUser=userService.registerNewPlayer(username, password);
            return ResponseEntity.ok(savedUser);
        }catch(IllegalArgumentException e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // THE FIX: Changed @RequestParam to @RequestBody to accept the JSON payload
    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody Map<String, String> credentials) {
        try {
            String username = credentials.get("username");
            String password = credentials.get("password");
            String token = userService.loginPlayer(username, password);

            // return it as a clean JSON object
            return ResponseEntity.ok(Map.of("token", token));
        } catch (IllegalArgumentException e) {
            // If the password is wrong, kick them out with a 401 Unauthorized status
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }

    @PostMapping("/{userId}/fund")
    public ResponseEntity<?> addFunds(@PathVariable Long userId, @RequestParam BigDecimal amount){
        User user= userRepository.findById(userId).orElseThrow(()->new IllegalArgumentException("User not found!"));
        user.setWalletBalance(user.getWalletBalance().add(amount));
        userRepository.save(user);

        return ResponseEntity.ok("successfullly added $"+amount+" to "+user.getUsername()+" 's wallet.New balance is: "+user.getWalletBalance());
    }

    // THE NEW PORTFOLIO WINDOW!
    @GetMapping("/{userId}/portfolio")
    public ResponseEntity<UserProfileDTO> getPortfolio(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getUserPortfolio(userId));
    }
}