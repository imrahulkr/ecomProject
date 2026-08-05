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

    @Value("${spring.app.jwtExpirationMs}")
    private int jwtExpirationMs;

    @Value("${spring.app.jwtCookieName}")
    private String jwtCookie;

    @Value("${app.cookie.secure:true}")
    private boolean cookieSecure;

    private final JwtConfig jwtConfig;
    private final SecureRandom secureRandom = new SecureRandom();

    public JwtUtils(JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
    }

    public String getJwtFromCookies(HttpServletRequest request) {
        Cookie cookie = WebUtils.getCookie(request, jwtCookie);
        if(cookie != null){
            return cookie.getValue();
        }
        return null;
    }

    public String getJwtFromHeader(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if(bearerToken != null && bearerToken.startsWith("Bearer ")){
            return bearerToken.substring(7);
        }
        return null;
    }

    public ResponseCookie generateJwtCookies(UserDetailsImpl userPrincipal) {
        String jwt = generateTokenFromUserDetails(userPrincipal);
        return  ResponseCookie.from(jwtCookie, jwt)
                .path("/api")
                .maxAge(24*60*60)
                .httpOnly(false)
                .secure(cookieSecure)
                .build();
    }

    // Use it for Logout / SignOut
    public ResponseCookie getJwtCleanCookies() {
        return ResponseCookie.from(jwtCookie, null)
                .path("/api")
                .build();
    }

    public String generateTokenFromUsername(String username) {
        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(key())
                .compact();
    }

    public String generateTokenFromUserDetails(UserDetailsImpl userPrincipal) {

        Instant now = Instant.now();
        Instant expiry = now.plus(jwtConfig.getAccessTokenTtlMinutes(), ChronoUnit.MINUTES);

        List<String> providers = userPrincipal.getOAuthAccounts().stream()
                .map(a -> a.getProvider())
                .toList();

        List<String> roles = userPrincipal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        return Jwts.builder()
                .subject(userPrincipal.getUsername())
                .claim("email", userPrincipal.getEmail())
                .claim("userId", userPrincipal.getId())
                .claim("providers", providers)
                .claim("roles", roles)
                .claim("enabled", userPrincipal.isEnabled())
                .issuer(jwtConfig.getIssuer())
                .audience().add("My-api").and()
                .issuedAt(Date.from(now))
                .expiration((Date.from(expiry)))
                .id(UUID.randomUUID().toString())
                .signWith(key())
                .compact();
    }

    public String getUserNameFromJwtToken(String token) {
        return Jwts.parser()
                        .verifyWith((SecretKey) key())
                .build().parseSignedClaims(token)
                .getPayload().getSubject();
    }

    private Key key() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(
                jwtConfig.getSecret()
        ));
    }

    public boolean validateJwtToken(String authToken) {
        try {
            Jwts.parser().verifyWith((SecretKey) key()).build().parseSignedClaims(authToken);
            return true;
        } catch (MalformedJwtException e) {
            logger.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            logger.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
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
