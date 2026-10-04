package com.ecommerce.project.security.dto;


/*
* Lightweight principal placed in the SecurityContext after JWT validation.
* Deliberately NOT a full UserDetails/DB-backed - we trust the JWT's claims for
* request-scoped authorization checks instead of hitting the DB on every request.
* For sensitive actoins, re-fetch the User from the DB.
* */

import com.ecommerce.project.security.OAuthAccount;
import org.springframework.security.core.GrantedAuthority;

import java.security.Principal;
import java.util.Collection;
import java.util.List;
import com.ecommerce.project.auth.User;

// implements Principal so Authentication.getName() resolves to the username - without it,
// AbstractAuthenticationToken.getName() falls through to principal.toString() (this record's
// auto-generated dump of every field) since a plain record satisfies none of the types
// getName() checks for. AuthUtil.loggedInUser() and AuthController.currentUsername both
// depend on getName() returning a real username.
public record JwtPrincipal(String userName, String email, Long userId,
                           Collection<? extends GrantedAuthority> authorities,
                           List<OAuthAccount> oAuthAccounts,  boolean enabled) implements Principal {
    @Override
    public String getName() {
        return userName;
    }
}
