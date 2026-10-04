package com.ecommerce.project.security.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.jwt")
public class JwtConfig {

    private String secret;
    private long accessTokenTtlMinutes = 10;
    private long refreshTokenTtlDays = 7;
    private String issuer = "http://localhost:8080";

    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }

    public long getAccessTokenTtlMinutes() { return accessTokenTtlMinutes; }
    public void setAccessTokenTtlMinutes(long accessTokenTtlMinutes) { this.accessTokenTtlMinutes = accessTokenTtlMinutes; }

    public long getRefreshTokenTtlDays() { return refreshTokenTtlDays; }
    public void setRefreshTokenTtlDays(long refreshTokenTtlDays) { this.refreshTokenTtlDays = refreshTokenTtlDays; }

    public String getIssuer() { return issuer; }
    public void setIssuer(String issuer) { this.issuer = issuer; }
}
