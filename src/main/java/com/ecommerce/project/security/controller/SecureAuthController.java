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
import com.ecommerce.project.service.RateLimiterService;
import com.ecommerce.project.service.RateLimiterService.Policy;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
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
    private final RateLimiterService rateLimiterService;

    @Value("${app.cookie.secure:true}")
    private boolean cookieSecure;

    public SecureAuthController(SecureAuthService secureAuthService, RefreshTokenService refreshTokenService, JwtUtils jwtUtils,
                                OneTimeExchangeCodeStore store, RateLimiterService rateLimiterService) {
        this.secureAuthService = secureAuthService;
        this.refreshTokenService = refreshTokenService;
        this.jwtUtils = jwtUtils;
        this.exchangeCodeStore = store;
        this.rateLimiterService = rateLimiterService;
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
    public ResponseEntity<?> signup(@Valid @RequestBody SignupRequest request, HttpServletRequest httpRequest) {
        rateLimiterService.consumeOrThrow(Policy.SIGNUP_IP, httpRequest.getRemoteAddr(),
                "Too many sign-up attempts. Please try again later.");
        return secureAuthService.signup(request);
    }

    // @PostMapping("/signup_secure")
    // public ResponseEntity<AuthResponse> signup(@Valid @RequestBody SignupRequest request) {
    //     SecureAuthServiceImpl.IssuedTokens tokens = secureAuthService.signup(request);
    //     return withRefreshCookie(tokens);
    // }

    // Two limits: attempts per IP (slows credential stuffing across many accounts) and failed
    // attempts per account (stops guessing one account's password from many IPs). A locked
    // account is rejected before the password is even checked, so guesses during the lockout
    // can't succeed; a successful login clears the account's failure count.
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        rateLimiterService.consumeOrThrow(Policy.LOGIN_IP, httpRequest.getRemoteAddr(),
                "Too many login attempts. Please try again later.");
        rateLimiterService.requireAvailable(Policy.LOGIN_FAILURES_PER_ACCOUNT, request.email(),
                "Too many failed login attempts for this account. Please try again later or reset your password.");
        SecureAuthServiceImpl.IssuedTokens tokens;
        try {
            tokens = secureAuthService.login(request);
        } catch (BadCredentialsException e) {
            rateLimiterService.record(Policy.LOGIN_FAILURES_PER_ACCOUNT, request.email());
            throw e;
        }
        rateLimiterService.reset(Policy.LOGIN_FAILURES_PER_ACCOUNT, request.email());
        return withRefreshCookie(tokens);
    }

    // refresh/logout authenticate with the refresh cookie alone, so a cross-site form could
    // trigger them (CSRF protection is off for the JWT API). Requiring a custom header closes
    // that: browsers only send one cross-origin after a CORS preflight, which only the allowed
    // frontend origins pass.
    private static void requireXhrHeader(HttpServletRequest request) {
        if (!"XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
            throw new AccessDeniedException("Missing X-Requested-With header");
        }
    }

    @PostMapping("/refresh_secure")
    public ResponseEntity<AuthResponse> refresh(HttpServletRequest request) {
        requireXhrHeader(request);
        String rawRefreshToken = extractRefreshCookie(request);
        var rotation = refreshTokenService.rotate(rawRefreshToken);

        AuthResponse body = secureAuthService.toAuthResponse(
                new SecureAuthServiceImpl.IssuedTokens(rotation.newAccessToken(), rotation.newRawRefreshToken(), rotation.user())
        );
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, buildRefreshCookie(rotation.newRawRefreshToken()).toString())
                .body(body);
    }

    @PostMapping("/logout_secure")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        requireXhrHeader(request);
        // Best-effort: revoke the presented refresh token's whole family.
        try{
            String rawRefreshToken = extractRefreshCookie(request);
            refreshTokenService.revokeByRawToken(rawRefreshToken);
        } catch (Exception ignored){
            // Already invalid/expired/missing -- nothing to do.
        }

        ResponseCookie clearCookie = ResponseCookie.from(REFRESH_COOKIE_NAME,"")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSecure ? "None" : "Lax")
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
        // SameSite=None + Secure in prod, since the deployed frontend is a separate origin/site.
        // Browsers reject SameSite=None without Secure, so local http dev (app.cookie.secure=false)
        // falls back to Lax -- frontend and backend are same-site (both "localhost", different port
        // only), so Lax still sends the cookie on our cross-origin XHR calls.
        return ResponseCookie.from(REFRESH_COOKIE_NAME, rawRefreshToken)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSecure ? "None" : "Lax")
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
