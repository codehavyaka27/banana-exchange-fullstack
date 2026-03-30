package com.bananatrading.engine.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtAuthFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // ==========================================
        // THE FIX: Let CORS "scout" requests (OPTIONS) pass through untouched!
        // ==========================================
        if (request.getMethod().equals("OPTIONS")) {
            filterChain.doFilter(request, response);
            return; // Exit the filter immediately so it doesn't look for a token
        }

        // 1. Look at the HTTP request and find the "Authorization" header
        String authHeader = request.getHeader("Authorization");

        // 2. Check if the user handed us a Bearer token
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            // Cut off the word "Bearer " to get just the raw gibberish token
            String token = authHeader.substring(7);

            try {
                // 3. Ask JwtUtil to check the math and open the token
                Claims claims = jwtUtil.extractAllClaims(token);
                String username = claims.getSubject();

                // 4. If the math is good and Spring doesn't already know who this is, log them in for this exact millisecond
                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(username, null, new ArrayList<>());

                    // Tell Spring Security: "This person is safe, let them trade."
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            } catch (Exception e) {
                // If the token is fake, expired, or tampered with, we just ignore it.
                // The filter chain will automatically block them later.
                System.out.println("Invalid VIP Pass: " + e.getMessage());
            }
        }

        // 5. Pass the request along to the next step
        filterChain.doFilter(request, response);
    }
}