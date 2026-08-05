package com.ecommerce.project.security.dto;


/*
* Lightweight principal placed in the SecurityContext after JWT validation.
* Deliberately NOT a full UserDetails/DB-backed - we trust the JWT's claims for
* request-scoped authorization checks instead of hitting the DB on every request.
* For sensitive actoins, re-fetch the User from the DB.
* */

import com.ecommerce.project.security.OAuthAccount;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.List;
import com.ecommerce.project.auth.User;

public record JwtPrincipal(String userName, String email, Long userId,
                           Collection<? extends GrantedAuthority> authorities,
                           List<OAuthAccount> oAuthAccounts,  boolean enabled) {
}
