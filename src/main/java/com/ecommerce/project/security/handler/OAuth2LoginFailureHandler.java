package com.ecommerce.project.security.handler;

import com.ecommerce.project.security.exception.AccountLinkingRequiredException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static com.ecommerce.project.config.AppConstants.FRONTEND_OAUTH_ERROR_URL;

@Component
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {

    private static final String FRONTEND_ERROR_URL = FRONTEND_OAUTH_ERROR_URL;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {

        // AccountLinkingRequiredException (and Similar) surface here wrapped by spring security.
        // Don't leak internal details - just tell frontend what to show the user.
        String reason = exception.getCause() instanceof AccountLinkingRequiredException
                ? "account_linking_required"
                : "oauth_login_failed";

        String encodedReason = URLEncoder.encode(reason, StandardCharsets.UTF_8);
        response.sendRedirect(FRONTEND_ERROR_URL + "?reason=" + encodedReason);
    }
}
