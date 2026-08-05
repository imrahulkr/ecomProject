package com.ecommerce.project.security.handler;

import com.ecommerce.project.auth.User;
import com.ecommerce.project.security.dto.OAuthUserInfo;
import com.ecommerce.project.security.jwt.JwtUtils;
import com.ecommerce.project.security.services.OAuthUserService;
import com.ecommerce.project.security.services.RefreshTokenService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final OAuthUserService oAuthUserService;
    private final JwtUtils jwtUtils;
    private final RefreshTokenService refreshTokenService;
    private final OneTimeExchangeCodeStore exchangeCodeStore;
    private final OAuth2AuthorizedClientService authorizedClientService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    public OAuth2LoginSuccessHandler(OAuthUserService oAuthUserService, JwtUtils jwtUtils, RefreshTokenService refreshTokenService,
                                     OneTimeExchangeCodeStore exchangeCodeStore, OAuth2AuthorizedClientService authorizedClientService) {

        this.oAuthUserService = oAuthUserService;
        this.jwtUtils = jwtUtils;
        this.refreshTokenService = refreshTokenService;
        this.exchangeCodeStore = exchangeCodeStore;
        this.authorizedClientService = authorizedClientService;
    }


    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        String registrationId = oauthToken.getAuthorizedClientRegistrationId();
        OAuth2User oAuth2User = oauthToken.getPrincipal();

        OAuth2AuthorizedClient authorizedClient = authorizedClientService
                .loadAuthorizedClient(registrationId, oAuth2User.getName());
        OAuth2AccessToken providerAccessToken = authorizedClient.getAccessToken();

        OAuthUserInfo userInfo = OAuthAttributeMapper.map(registrationId, oAuth2User, providerAccessToken);

        User user = oAuthUserService.findOrCreateFromOAuth(userInfo);

        String accessToken = jwtUtils.generateAccessToken(user);
        String refreshToken = refreshTokenService.issueNewChain(user);

        /*
        * Refresh token as httpOnly cookie immediately - no need to round-trip this one.
        * SameSite = None because the React frontEnd is a separate origin; Strict/Lax
        * cookies are not sent on cross-origin fetch/XHR requests at all.
        * Require Secure (HTTPS) - browsers reject SameSite=None without it.
        * */

        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/api/auth")
                .maxAge(Duration.ofDays(jwtUtils.getRefreshTokenTtlDays()))
                .build();

        response.addHeader("Set-Cookie", cookie.toString());

        //Access token goes through a one-time exchange code, not directly in the URL.
        String exchangeCode = exchangeCodeStore.store(accessToken);

        response.sendRedirect(frontendUrl + "/oauth/callback?code=" + exchangeCode);
    }
}

