package com.cs203.tariffg4t2.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class JwtService {
    
    private final Key key;
    private final long expirationMs;

    public JwtService(@Value("${jwt.secret}") String secret,
                      @Value("${jwt.expiration-ms}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.expirationMs = expirationMs;
    }

    public String generateToken(Long userId, String username, String email, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("email", email);
        claims.put("role", role);

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)  // Set username as subject (standard JWT practice)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // parse and validate token
    public Jws<Claims> parseToken(String token) throws JwtException {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token);
    }

    // check if token is valid
    public boolean isTokenValid(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    // extract username from token
    public String getUsername(String token) {
        return parseToken(token).getBody().getSubject();
    }

    // extract userId from token
    public Long getUserId(String token) {
        Claims claims = parseToken(token).getBody();
        Object userIdObj = claims.get("userId");

        if (userIdObj instanceof Integer) {
            return ((Integer) userIdObj).longValue();
        } else if (userIdObj instanceof Long) {
            return (Long) userIdObj;
        } else if (userIdObj instanceof String) {
            return Long.parseLong((String) userIdObj);
        } 

        return null;
    }

    // extract email from token
    public String getEmail(String token) {
        return (String) parseToken(token).getBody().get("email");
    }

    // extract role from token
    public String getRole(String token) {
        return (String) parseToken(token).getBody().get("role");
    }

    // check if token is expired
    public boolean isTokenExpired(String token) {
        try {
            Date expiryDate = parseToken(token).getBody().getExpiration();
            return expiryDate.before(new Date());
        } catch (JwtException e) {
            return false;
        }
    }
}
