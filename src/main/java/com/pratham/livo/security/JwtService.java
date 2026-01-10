package com.pratham.livo.security;

import com.pratham.livo.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class JwtService {

    @Value("${jwt.access.secret}")
    private String accessSecret;

    @Value("${jwt.refresh.secret}")
    private String refreshSecret;

    @Value("${jwt.access.expiry}")
    private Long accessExpiry;

    @Value("${jwt.refresh.expiry}")
    private Long refreshExpiry;

    private SecretKey getAccessSigningKey(){
        return Keys.hmacShaKeyFor(accessSecret.getBytes(StandardCharsets.UTF_8));
    }

    private SecretKey getRefreshSigningKey(){
        return Keys.hmacShaKeyFor(refreshSecret.getBytes(StandardCharsets.UTF_8));
    }

    //generate access token for the user
    public String generateAccessToken(User user, String jti){
        return Jwts.builder()
                .id(jti)
                .subject(String.valueOf(user.getId()))
                .claim("name",user.getName())
                .claim("email",user.getEmail())
                .claim("roles",user.getRoles().stream().map(
                        role -> role.name()
                ).toList())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessExpiry))
                .signWith(getAccessSigningKey())
                .compact();
    }

    //generate refresh token for the user
    public String generateRefreshToken(Long userId){
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshExpiry))
                .signWith(getRefreshSigningKey())
                .compact();
    }

    public Long getUserIdFromRefreshToken(String token){
        Claims claims = Jwts.parser().verifyWith(getRefreshSigningKey()).build()
                .parseSignedClaims(token)
                .getPayload();

        return Long.valueOf(claims.getSubject());
    }


}
