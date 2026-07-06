package com.example.seckill.util;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {

    //密钥
    private static final String SECRET = "your-256-bit-secret-key-for-jwt-authentication-2026";

    //Token过期时间（7天）
    private static final long EXPIRATION = 7 * 24 * 60 * 60 * 1000L;

    private Key getSigningKey(){
        byte[] keyBytes=SECRET.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 生成Token
     * @param userId
     * @param username
     * @param role
     * @return
     */
    public String generateToken(Long userId,String username,String role){
        Map<String,Object> claims=new HashMap<>();
        claims.put("userId",userId);
        claims.put("username",username);
        claims.put("role",role);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis()+EXPIRATION))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 从Token中获取Claims
     */
    public Claims getClaims(String token){
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 验证Token
     */
    public boolean validateToken(String token){
        try{
            Claims claims=getClaims(token);
            return claims.getExpiration().after(new Date());
        }catch (Exception e){
            return false;
        }
    }

    /**
     * 从Token中获取用户ID
     */
    public Long getUserId(String token){
        return getClaims( token).get("userId",Long.class);
    }

    /**
     * 从Token中获取用户名
     */
    public String getUsername(String token){
        return getClaims(token).getSubject();
    }

    /**
     * 从Token中获取用户角色
     */
    public String getRole(String token){
        return getClaims(token).get("role",String.class);
    }


}
