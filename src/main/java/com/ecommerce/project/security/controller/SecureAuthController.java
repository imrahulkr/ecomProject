package com.ecommerce.project.security.controller;

import com.ecommerce.project.security.dto.AuthResponse;
import com.ecommerce.project.security.dto.LoginRequest;
import com.ecommerce.project.security.dto.SignupRequest;
import com.ecommerce.project.security.exception.InvalidRefreshTokenException;
import com.ecommerce.project.security.handler.OneTimeExchangeCodeStore;
import com.ecommerce.project.security.jwt.JwtUtils;
import com.ecommerce.project.security.services.RefreshTokenService;
import com.ecommerce.project.security.services.SecureAuthService;
import com.ecommerce.project.security.services.SecureAuthServiceImpl;
import com.ecommerce.project.auth.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class SecureAuthController {
    private static final String REFRESH_COOKIE_NAME  = "refreshToken";
    private static final String REFRESH_COOKIE_PATH = "/api/auth";

    private final SecureAuthService secureAuthService;
    private final RefreshTokenService refreshTokenService;
    private final JwtUtils jwtUtils;
    private final OneTimeExchangeCodeStore exchangeCodeStore;

    public SecureAuthController(SecureAuthService secureAuthService, RefreshTokenService refreshTokenService, JwtUtils jwtUtils, OneTimeExchangeCodeStore store) {
        this.secureAuthService = secureAuthService;
        this.refreshTokenService = refreshTokenService;
        this.jwtUtils = jwtUtils;
        this.exchangeCodeStore = store;
    }

    /*
    * Called by the frontend right after an OAuth2 redirect. Trades the short-lived one-time code
    * (from the redirect URL) for the real access token, delivered in the response body -- never in URL.
    * */

    @PostMapping("/exchange")
    public ResponseEntity<Map<String, String>> exchange(@RequestBody Map<String, String> body) {
        String code = body.get("code");
        String accessToken = exchangeCodeStore.consume(code);
        return ResponseEntity.ok(Map.of("accessToken", accessToken));
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody SignupRequest request) {
        return secureAuthService.signup(request);
    }

    // @PostMapping("/signup_secure")
    // public ResponseEntity<AuthResponse> signup(@Valid @RequestBody SignupRequest request) {
    //     SecureAuthServiceImpl.IssuedTokens tokens = secureAuthService.signup(request);
    //     return withRefreshCookie(tokens);
    // }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        SecureAuthServiceImpl.IssuedTokens tokens = secureAuthService.login(request);
        return withRefreshCookie(tokens);
    }

    @PostMapping("/refresh_secure")
    public ResponseEntity<AuthResponse> refresh(HttpServletRequest request) {
        String rawRefreshToken = extractRefreshCookie(request);
        var rotation = refreshTokenService.rotate(rawRefreshToken);

        String newAccessToken = jwtUtils.generateAccessToken(rotation.user());
        AuthResponse body = secureAuthService.toAuthResponse(
                new SecureAuthServiceImpl.IssuedTokens(newAccessToken, rotation.newRawRefreshToken(), rotation.user())
        );
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, buildRefreshCookie(rotation.newRawRefreshToken()).toString())
                .body(body);
    }

    @PostMapping("/logout_secure")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        // Best-effort: revoke the presented refresh token's whole family.
        try{
            String rawRefreshToken = extractRefreshCookie(request);
            refreshTokenService.revokeByRawToken(rawRefreshToken);
        } catch (Exception ignored){
            // Already invalid/expired/missing -- nothing to do.
        }

        ResponseCookie clearCookie = ResponseCookie.from(REFRESH_COOKIE_NAME,"")
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .maxAge(0)
                .build();

        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, clearCookie.toString()).build();
    }



    private ResponseEntity<AuthResponse> withRefreshCookie(SecureAuthServiceImpl.IssuedTokens tokens) {
        AuthResponse body = secureAuthService.toAuthResponse(tokens);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, buildRefreshCookie(tokens.refreshToken()).toString())
                .body(body);
    }


    private ResponseCookie buildRefreshCookie(String rawRefreshToken) {
        // SameSite=None + Secure required since the react frontend is a separate origin
        // -- Strict/Lax cookies are silently dropped on cross-origin calls.

        return ResponseCookie.from(REFRESH_COOKIE_NAME, rawRefreshToken)
                .httpOnly(true)
                .secure(true) // requires HTTPS -- use false only in local http dev
                .sameSite("None")
                .path(REFRESH_COOKIE_PATH)
                .maxAge(java.time.Duration.ofDays(jwtUtils.getRefreshTokenTtlDays()))
                .build();
    }

    private String extractRefreshCookie(HttpServletRequest request) {
        if(request.getCookies() == null){
            throw new InvalidRefreshTokenException("No refresh token cookie present");
        }
        for(var cookie : request.getCookies()){
            if(REFRESH_COOKIE_NAME.equals(cookie.getName())){
                return cookie.getValue();
            }
        }
        throw new InvalidRefreshTokenException("No refresh token cookie present");
    }
}
