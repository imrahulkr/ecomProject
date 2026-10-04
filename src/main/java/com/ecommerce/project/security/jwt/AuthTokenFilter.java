package com.ecommerce.project.security.jwt;

import com.ecommerce.project.security.OAuthAccount;
import com.ecommerce.project.security.dto.JwtPrincipal;
import com.ecommerce.project.security.services.UserDetailsServiceImpl;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

//JWTAuthenticationFilter
@Component
@RequiredArgsConstructor
public class AuthTokenFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final UserDetailsServiceImpl userDetailsService;
    private static final Logger logger = LoggerFactory.getLogger(AuthTokenFilter.class);

    // Authorities come from the database on every request, not from the token's "roles" claim,
    // so a revoked role or a disabled/deleted account takes effect immediately rather than when
    // the access token expires. Any problem leaves the request unauthenticated; protected routes
    // then answer 401 via AuthEntryPointJwt.
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                authenticate(header.substring(7));
            } catch (ExpiredJwtException e) {
                // Routine - the client refreshes and retries.
                SecurityContextHolder.clearContext();
                logger.debug("Expired access token for {}", request.getRequestURI());
            } catch (JwtException | IllegalArgumentException | UsernameNotFoundException e) {
                SecurityContextHolder.clearContext();
                logger.warn("Rejected access token for {}: {}", request.getRequestURI(), e.getMessage());
            }
        }
        filterChain.doFilter(request, response);
    }

    @SuppressWarnings("unchecked")
    private void authenticate(String token) {
        Claims claims = jwtUtils.parseAndValidate(token);
        String username = claims.getSubject();

        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        if (!userDetails.isEnabled() || !userDetails.isAccountNonLocked()) {
            logger.debug("Access token presented for disabled/locked account {}", username);
            return;
        }

        JwtPrincipal principal = new JwtPrincipal(
                username,
                claims.get("email", String.class),
                claims.get("userId", Long.class),
                userDetails.getAuthorities(),
                (List<OAuthAccount>) claims.get("oAuthAccounts", List.class),
                true);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, userDetails.getAuthorities()));
    }
}
