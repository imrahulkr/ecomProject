package com.ecommerce.project.security.handler;


/*
* Providers don't agree on attribute names:
* - Google: sub (id), email, email_verified, name
* - GitHub: id (numeric), email (often null if private), name or login
*
* GitHub's main profile frequently omits email if the user has it set ot private, so we fall back
* to calling GitHub's /user/emails endpoint with the provider access token to find the verified
* primary email.
* */

import com.ecommerce.project.security.dto.OAuthUserInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

public class OAuthAttributeMapper {
    private static final Logger log = LoggerFactory.getLogger(OAuthAttributeMapper.class);
    private static final RestClient GITHUB_CLIENT = RestClient.create("https://api.github.com");

    private OAuthAttributeMapper() {}

    public static OAuthUserInfo map(String registeredId, OAuth2User oAuth2User, OAuth2AccessToken accessToken) {
        return switch (registeredId){
            case "google" -> mapGoogle(oAuth2User);
            case "github" -> mapGithub(oAuth2User, accessToken);
            default -> throw new IllegalArgumentException("Unsupported Provider : " +  registeredId);
        };
    }

    private static OAuthUserInfo mapGoogle(OAuth2User oAuth2User) {
        return new OAuthUserInfo(
                "google",
                oAuth2User.getAttribute("sub"),
                oAuth2User.getAttribute("email"),
                Boolean.TRUE.equals(oAuth2User.getAttribute("email_verified")),
                oAuth2User.getAttribute("name")
        );
    }

    private static OAuthUserInfo mapGithub(OAuth2User oAuth2User, OAuth2AccessToken accessToken) {
        Object idAttr = oAuth2User.getAttribute("id");
        String providerUserId = idAttr != null ? idAttr.toString() : null;

        String name = oAuth2User.getAttribute("name");
        if(name == null){
            name = oAuth2User.getAttribute("login");
        }

        GithubEmail primaryEmail = fetchPrimaryVerifiedEmail(accessToken);

        String email = primaryEmail != null ? primaryEmail.email() : oAuth2User.getAttribute("email");
        boolean emailVerified = primaryEmail != null && primaryEmail.verified();

        return new OAuthUserInfo("github", providerUserId, email, emailVerified, name);
    }

    /*
    * Calls GET /user/emails and picks the primary, verified entry.
    * Requires the "user:email" scope (already configured in application.properties).
    * Returns null if the call fails or no verified primary email is found -
    * callers must treat that as "cannot confirm ownership of this email".
    * */

    @SuppressWarnings("unchecked")
    private static GithubEmail fetchPrimaryVerifiedEmail(OAuth2AccessToken accessToken){
        try{
            List<Map<String, Object>> emails = GITHUB_CLIENT.get()
                    .uri("/user/emails")
                    .header("Authorization", "Bearer" + accessToken.getTokenValue())
                    .header("Accept", "application/vnd.github+json")
                    .retrieve()
                    .body(List.class);

            if(emails == null || emails.isEmpty()) return null;

            for(Map<String, Object> entry : emails){
                boolean primary = Boolean.TRUE.equals(entry.get("primary"));
                boolean verified = Boolean.TRUE.equals(entry.get("verified"));
                if(primary && verified){
                    return new GithubEmail((String) entry.get("email"), true);
                }
            }
            // No verified primary found - don't silently trust an unverified one.
            return null;
        } catch (Exception e){
            log.warn("Failed to fetch GitHub user emails; treating email as unverified", e);
            return null;
        }
    }

    private record GithubEmail(String email, boolean verified) {}
}