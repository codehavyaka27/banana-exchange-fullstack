package com.bananatrading.engine.seeder;
import com.bananatrading.engine.entity.Card;
import com.bananatrading.engine.entity.User;
import com.bananatrading.engine.repository.UserRepository;
import com.bananatrading.engine.service.CardService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class MarketSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CardService cardService;
    private final PasswordEncoder passwordEncoder;
    public MarketSeeder(UserRepository userRepository, CardService cardService, PasswordEncoder passwordEncoder){
        this.userRepository=userRepository;
        this.cardService=cardService;
        this.passwordEncoder = passwordEncoder;

    }

    public void run(String...args){
        if(userRepository.findByusername("SystemAdmin").isEmpty()){
            System.out.println("market seeder intializing!");

            User admin=new User();
            admin.setUsername("SystemAdmin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setWalletBalance(new BigDecimal("10000000.00"));
            admin=userRepository.save(admin);
            //High liquidity ,slow moves
            cardService.mintNewCard(admin.getId(),"Tiko",100000,new BigDecimal("500000.00"));
            //medium liquidity,medium risk
            cardService.mintNewCard((admin.getId()),"ABM",50000,new BigDecimal("50000.00"));
            //high risk&reward
            cardService.mintNewCard((admin.getId()),"Curse",10000,new BigDecimal("5000.00"));


            System.out.println("MARKET SEEDED SUCCESSFULLY!...");



        }else{
            System.out.println("MARKET IS SLREADY SEEDED");
        }
    }

}
