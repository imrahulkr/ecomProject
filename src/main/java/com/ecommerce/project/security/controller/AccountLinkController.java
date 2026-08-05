package com.ecommerce.project.security.controller;



/*
* Explicit, authenticated account linking - the safe path we fall back to whenever email-based
* auto linking durig login isn't appropriate (e.g. provider didn't confirm email verification).
*
* NOTE: actually kicking off the OAuth2 flow "to link" (rather than "to log in")
* typically means redirecting to /oauth2/authorization/{provider} with a marker
* (e.g. a signed state param or session attribute) indicating "link to current user"
* rather than "find or create a user". Wire that distinction into your success handler if you
* need in-app linking initiated from a logged-in session.
* */

import com.ecommerce.project.security.OAuthAccount;
import com.ecommerce.project.auth.User;
import com.ecommerce.project.security.OAuthAccountRepository;
import com.ecommerce.project.auth.UserRepository;
import com.ecommerce.project.security.dto.JwtPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/account")
public class AccountLinkController {

    private final UserRepository userRepository;
    private final OAuthAccountRepository oAuthAccountRepository;

    public AccountLinkController(UserRepository userRepository, OAuthAccountRepository oAuthAccountRepository) {
        this.userRepository = userRepository;
        this.oAuthAccountRepository = oAuthAccountRepository;
    }

    @GetMapping("/linked-accounts")
    public ResponseEntity<Map<String, Object>> getLinkedAccounts(@AuthenticationPrincipal JwtPrincipal principal) {
        User user = userRepository.findById(principal.userId())
                .orElseThrow(() -> new AccessDeniedException("User not found"));

        List<String> providers = user.getOAuthAccounts().stream().map(OAuthAccount::getProvider).toList();

        return ResponseEntity.ok(Map.of(
                "hasPassword", user.hasPassword(),
                "linkedProviders", providers
        ));
    }

    /*
    * Unlink a provider - refuses if it would leave the user with No way to log in
    * (no Password and This is their only linked provider).
    * */

    @DeleteMapping("/link/{provider}")
    public ResponseEntity<Void> deleteLinkedAccount(@AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable("provider") String provider) {

        User user = userRepository.findById(principal.userId())
                .orElseThrow(() -> new AccessDeniedException("User not found"));

        boolean wouldHaveNoLoginMethodLeft = !user.hasPassword() && user.getOAuthAccounts().size() <= 1;

        if(wouldHaveNoLoginMethodLeft) {
            throw new IllegalStateException("Cannot unlink your only sign-in method. Set a password first, or link another provider.");
        }

        oAuthAccountRepository.deleteByUserAndProvider(user, provider);
//        notificationService.notiyProviderUnlinked(user, provider);
        return ResponseEntity.noContent().build();
    }
}
