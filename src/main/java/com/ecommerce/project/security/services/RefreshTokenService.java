package com.ecommerce.project.security.services;

import com.ecommerce.project.security.RefreshToken;
import com.ecommerce.project.auth.User;
import com.ecommerce.project.security.RefreshTokenRepository;
import com.ecommerce.project.security.exception.InvalidRefreshTokenException;
import com.ecommerce.project.security.jwt.JwtUtils;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.UUID;


@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtils jwtUtils;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, JwtUtils jwtUtils) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtUtils = jwtUtils;
    }

    /* Issue a brand new refresh token chain (new family) - call this at login. */
    @Transactional
    public String issueNewChain(User user){
        UUID familyId = UUID.randomUUID();
        return issueToken(user, familyId);
    }

    /*
    * Rotates a refresh token: validates the presented raw token, and if valid,
    * revokes it and issues a new one in the same family. If the presented token was already
    * used/revoked, that's a signal of theft -- revoke the ENTIRE family so every token
    * derived from the login chain stops working immediately.
    * */
    @Transactional
    public RotationResult rotate(String rawPresentedToken) {
        String hash  = hash(rawPresentedToken);
        RefreshToken existing  = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new InvalidRefreshTokenException("RefreshToken not recognized"));

        if(existing.isRevoked()){
            // Reuse of an already/revoked token - treat as compromise.
            refreshTokenRepository.revokeAllByFamilyId(existing.getFamilyId());
            throw new InvalidRefreshTokenException("Refresh token reuse detected - " +
                    "all sessions in this chain has been revoked. Please log in again.");
        }

        if(existing.isEspired()){
            throw new InvalidRefreshTokenException("Refresh token expired");
        }

        existing.setRevoked(true);
        refreshTokenRepository.save(existing);

        String newRawToken = issueToken(existing.getUser(), existing.getFamilyId());
        return new RotationResult(existing.getUser(), newRawToken);
    }



    @Transactional
    public void revodeAllForUser(Long userId){
        refreshTokenRepository.revokeAllByUserId(userId);
    }


    // Used at logout -- revokes just the presented token's family. not the whole user.
    @Transactional
    public void revokeByRawToken(String rawToken){
        refreshTokenRepository.findByTokenHash(hash(rawToken))
                .ifPresent( t -> refreshTokenRepository.revokeAllByFamilyId(t.getFamilyId()));
    }


    private String issueToken(User user, UUID familyId){
        String rawToken = jwtUtils.generateRawRefreshToken();
        RefreshToken entity = RefreshToken.builder()
                .user(user)
                .tokenHash(hash(rawToken))
                .familyId(familyId)
                .expiresAt(Instant.now().plus(jwtUtils.getRefreshTokenTtlDays(), ChronoUnit.DAYS))
                .revoked(false)
                .build();
        refreshTokenRepository.save(entity);
        return rawToken;
    }

    private String hash(String raw){
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e){
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    public record RotationResult(User user, String newRawRefreshToken) {}
}
