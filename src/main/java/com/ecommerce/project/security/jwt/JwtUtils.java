package com.ecommerce.project.security.jwt;

import com.ecommerce.project.auth.User;
import com.ecommerce.project.security.config.JwtConfig;
import com.ecommerce.project.security.services.UserDetailsImpl;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

import javax.crypto.SecretKey;
import java.security.Key;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;

// JWT Service

@Component
public class JwtUtils {
    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

    @Value("${app.cookie.secure:true}")
    private boolean cookieSecure;

    private final JwtConfig jwtConfig;
    private final SecureRandom secureRandom = new SecureRandom();

    public JwtUtils(JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
    }

    // Use it for Logout / SignOut
    private Key key() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(
                jwtConfig.getSecret()
        ));
    }

    // ------------------------------------------------------------------------------
    // ==============================================================================
    // ------------------------------------------------------------------------------


   public String generateAccessToken(User user){
        Instant now = Instant.now();
        Instant expiry = now.plus(jwtConfig.getAccessTokenTtlMinutes(), ChronoUnit.MINUTES);

        List<String> providers = user.getOAuthAccounts().stream()
                .map(a -> a.getProvider())
                .toList();

       List<String> roles = user.getRoles().stream()
               .map(role -> role.getRoleName().name())
               .toList();

        return Jwts.builder()
                .subject(user.getUsername())
                .claim("email", user.getEmail())
                .claim("userId", user.getUserId())
                .claim("providers", providers)
                .claim("roles", roles)
                .claim("enabled", user.isEnabled())
                .issuer(jwtConfig.getIssuer())
                .audience().add("My-api").and()
                .issuedAt(Date.from(now))
                .expiration((Date.from(expiry)))
                .id(UUID.randomUUID().toString())
                .signWith(key())
                .compact();
   }

   public Claims parseAndValidate(String token){
        return Jwts.parser()
                .verifyWith((SecretKey) key())
                .requireIssuer(jwtConfig.getIssuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
   }

   public long getAccessTokenTtlSeconds(){
        return jwtConfig.getAccessTokenTtlMinutes() * 60;
   }

   /*
   * Raw refresh token - opaque, high-entropy, Not a JWT, only its hash
   * is ever persisted (See RefreshTokenService), and only raw valu
   * is ever sent to the client, as an httpOnly cookie.
   * */

    public String generateRawRefreshToken(){
        byte[] bytes = new byte[64];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public long getRefreshTokenTtlDays(){
        return jwtConfig.getRefreshTokenTtlDays();
    }
}
