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
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.UUID;


@Service
public class RefreshTokenService {

    // A reused token revoked more recently than this is treated as a legitimate concurrent
    // duplicate (e.g. React StrictMode double-invoking an effect, or two browser tabs racing
    // a silent refresh) rather than theft -- see rotate() below.
    private static final Duration REUSE_GRACE_WINDOW = Duration.ofSeconds(10);

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
            boolean withinGraceWindow = existing.getUpdatedAt() != null
                    && existing.getUpdatedAt().isAfter(Instant.now().minus(REUSE_GRACE_WINDOW));
            if (withinGraceWindow) {
                // Almost certainly a duplicate of the request that already rotated this token,
                // not a replayed/stolen one -- reject just this request and leave the chain
                // (including whichever token the winning request just issued) intact.
                throw new InvalidRefreshTokenException(
                        "Refresh already in progress from another request. Please retry.");
            }
            // Reuse of a token revoked well outside the grace window - treat as compromise.
            refreshTokenRepository.revokeAllByFamilyId(existing.getFamilyId());
            throw new InvalidRefreshTokenException("Refresh token reuse detected - " +
                    "all sessions in this chain has been revoked. Please log in again.");
        }

        if(existing.isEspired()){
            throw new InvalidRefreshTokenException("Refresh token expired");
        }

        // Atomic conditional revoke -- 0 affected rows means a concurrent request revoked this
        // exact token in the window between our SELECT above and this UPDATE. That's always a
        // benign near-simultaneous duplicate, never a genuine stale replay (a truly stale token
        // would already have shown revoked=true above and been caught by the grace-window check),
        // so reject just this caller rather than treating it as theft.
        int updated = refreshTokenRepository.revokeIfActive(existing.getId());
        if (updated == 0) {
            throw new InvalidRefreshTokenException(
                    "Refresh already in progress from another request. Please retry.");
        }

        String newRawToken = issueToken(existing.getUser(), existing.getFamilyId());

        // Generate the access token here, still inside the open session -- User.oAuthAccounts
        // is a lazy @OneToMany, and with spring.jpa.open-in-view=false the session closes the
        // moment this method returns. Touching that lazy collection here (via
        // generateAccessToken -> user.getOAuthAccounts()) initializes it while it's still safe
        // to do so, so later reads of the same User instance (e.g. SecureAuthServiceImpl
        // .toAuthResponse, called after this transaction has committed) don't hit
        // LazyInitializationException.
        String newAccessToken = jwtUtils.generateAccessToken(existing.getUser());
        return new RotationResult(existing.getUser(), newRawToken, newAccessToken);
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

    public record RotationResult(User user, String newRawRefreshToken, String newAccessToken) {}
}
