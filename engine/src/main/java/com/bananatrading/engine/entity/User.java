package com.bananatrading.engine.entity;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name="users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true,nullable = false)
    private String username;

    @Column(nullable = false)
    private BigDecimal walletBalance;

    @Column(nullable = false)
    private String password;

    public User(){
    }

    public Long getId(){
        return id;
    }

    public void setId(Long id){
        this.id=id;
    }

    public String getUsername(){
        return username;
    }

    public void setUsername(String username){
        this.username=username;
    }

    public BigDecimal getWalletBalance(){
        return walletBalance;
    }

    public void setWalletBalance(BigDecimal walletBalance){
        this.walletBalance=walletBalance;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password){
        this.password=password;
    }
}
